package com.logcollector.collector.service;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.logcollector.collector.kafka.RawLogKafkaProducer;
import com.logcollector.collector.position.CollectionPositionTracker;
import com.logcollector.collector.ssh.SSHCommandExecutor;
import com.logcollector.collector.ssh.SSHConnectionManager;
import com.logcollector.config.security.EncryptionUtil;
import com.logcollector.domain.model.ServerConfiguration;
import com.logcollector.domain.repository.ServerConfigurationRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Main service for log collection from remote servers.
 *
 * Orchestrates SSH connections, incremental position tracking,
 * and publishing to Kafka.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LogCollectionService {

    private final SSHConnectionManager sshConnectionManager;
    private final SSHCommandExecutor sshCommandExecutor;
    private final CollectionPositionTracker positionTracker;
    private final RawLogKafkaProducer kafkaProducer;
    private final ServerConfigurationRepository serverRepository;
    private final EncryptionUtil encryptionUtil;
    private final MeterRegistry meterRegistry;

    // Metrics
    private Counter linesCollectedCounter;
    private Counter collectionErrorsCounter;
    private Timer collectionTimer;

    /**
     * Collects logs from a single server and publishes to Kafka.
     *
     * @param server server configuration
     */
    @Transactional
    public void collectLogsFromServer(ServerConfiguration server) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("serverId", server.getId().toString());
        MDC.put("hostname", server.getHostname());

        Timer.Sample timerSample = Timer.start(meterRegistry);

        try {
            log.info("Starting log collection from server {}", server.getHostname());

            // Decrypt password
            String password = encryptionUtil.decrypt(server.getEncryptedPassword());

            // Establish SSH connection
            Session session = sshConnectionManager.connect(
                    server.getHostname(),
                    server.getPort(),
                    server.getUsername(),
                    password
            );

            // Update server status to CONNECTED
            updateServerStatus(server, ServerConfiguration.ServerStatus.CONNECTED, null);

            // Collect from each log file
            for (String logFilePath : server.getLogFilePaths()) {
                collectFromLogFile(server, session, logFilePath, correlationId);
            }

            // Update last collection timestamp
            server.setLastCollectionAt(LocalDateTime.now());
            serverRepository.save(server);

            log.info("Successfully collected logs from server {}", server.getHostname());

        } catch (JSchException e) {
            log.error("SSH connection failed for server {}", server.getHostname(), e);
            updateServerStatus(server, ServerConfiguration.ServerStatus.ERROR,
                    "SSH connection failed: " + e.getMessage());
            incrementErrorCounter();
            throw new LogCollectionException("Failed to connect to server", e);

        } catch (Exception e) {
            log.error("Log collection failed for server {}", server.getHostname(), e);
            updateServerStatus(server, ServerConfiguration.ServerStatus.ERROR,
                    "Collection failed: " + e.getMessage());
            incrementErrorCounter();
            throw new LogCollectionException("Failed to collect logs", e);

        } finally {
            timerSample.stop(getCollectionTimer());
            MDC.clear();
        }
    }

    /**
     * Collects logs from a single file on the server.
     *
     * @param server server configuration
     * @param session SSH session
     * @param logFilePath log file path
     * @param correlationId correlation ID for tracing
     */
    private void collectFromLogFile(ServerConfiguration server, Session session,
                                     String logFilePath, String correlationId)
            throws JSchException, IOException {

        log.debug("Collecting from file: {}", logFilePath);

        // Get current file line count
        long currentLineCount = sshCommandExecutor.getLineCount(session, logFilePath);

        // Get last collected position
        long startLine = positionTracker.getLastPosition(server.getId(), logFilePath)
                .map(pos -> pos + 1) // Start from next line after last position
                .orElse(1L); // Start from beginning if never collected

        // Check if there are new lines
        if (startLine > currentLineCount) {
            log.debug("No new lines in {} (last position: {}, current lines: {})",
                    logFilePath, startLine - 1, currentLineCount);
            return;
        }

        long linesToCollect = currentLineCount - startLine + 1;
        log.info("Collecting {} new lines from {} (starting at line {})",
                linesToCollect, logFilePath, startLine);

        // Collect in batches to avoid memory issues
        int batchSize = server.getBatchSize();
        long currentPosition = startLine;

        while (currentPosition <= currentLineCount) {
            int linesToRead = (int) Math.min(batchSize, currentLineCount - currentPosition + 1);

            // Read batch of lines
            List<String> logLines = sshCommandExecutor.tailLogFile(
                    session, logFilePath, currentPosition, linesToRead
            );

            if (logLines.isEmpty()) {
                break;
            }

            // Publish to Kafka
            kafkaProducer.sendRawLogs(
                    server,
                    logFilePath,
                    logLines,
                    correlationId
            );

            // Update position
            currentPosition += logLines.size();
            positionTracker.updatePosition(server.getId(), logFilePath, currentPosition - 1);

            // Update metrics
            incrementLinesCollectedCounter(logLines.size());

            log.debug("Collected batch of {} lines from {} (now at position {})",
                    logLines.size(), logFilePath, currentPosition - 1);
        }

        log.info("Completed collection from {} - collected {} lines total",
                logFilePath, linesToCollect);
    }

    /**
     * Updates server status and error message.
     *
     * @param server server configuration
     * @param status new status
     * @param errorMessage error message (null if no error)
     */
    private void updateServerStatus(ServerConfiguration server,
                                     ServerConfiguration.ServerStatus status,
                                     String errorMessage) {
        server.setStatus(status);
        server.setLastErrorMessage(errorMessage);
        serverRepository.save(server);
    }

    private void incrementLinesCollectedCounter(int count) {
        if (linesCollectedCounter == null) {
            linesCollectedCounter = Counter.builder("log.collection.lines.collected")
                    .description("Number of log lines collected")
                    .register(meterRegistry);
        }
        linesCollectedCounter.increment(count);
    }

    private void incrementErrorCounter() {
        if (collectionErrorsCounter == null) {
            collectionErrorsCounter = Counter.builder("log.collection.errors")
                    .description("Number of log collection errors")
                    .register(meterRegistry);
        }
        collectionErrorsCounter.increment();
    }

    private Timer getCollectionTimer() {
        if (collectionTimer == null) {
            collectionTimer = Timer.builder("log.collection.duration")
                    .description("Log collection duration")
                    .register(meterRegistry);
        }
        return collectionTimer;
    }

    /**
     * Exception thrown when log collection fails.
     */
    public static class LogCollectionException extends RuntimeException {
        public LogCollectionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
