package com.logcollector.collector.kafka;

import com.logcollector.kafka.RawLogMessage;
import com.logcollector.domain.model.ServerConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Publishes raw log messages to Kafka.
 *
 * Sends collected logs to the raw-logs topic for downstream processing.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RawLogKafkaProducer {

    @Value("${log-collection.kafka.raw-logs-topic:raw-logs}")
    private String rawLogsTopic;

    private final KafkaTemplate<String, RawLogMessage> kafkaTemplate;

    /**
     * Sends a batch of raw log lines to Kafka.
     *
     * @param server server configuration
     * @param logFilePath log file path
     * @param logLines log content lines
     * @param correlationId correlation ID for tracing
     */
    public void sendRawLogs(ServerConfiguration server, String logFilePath, List<String> logLines,
                            String correlationId) {

        log.debug("Sending {} log lines to Kafka topic {}", logLines.size(), rawLogsTopic);

        for (String logLine : logLines) {
            sendRawLog(server, logFilePath, logLine, correlationId);
        }

        log.debug("Successfully sent {} log lines to Kafka", logLines.size());
    }

    /**
     * Sends a single raw log line to Kafka.
     *
     * @param server server configuration
     * @param logFilePath log file path
     * @param logLine log content
     * @param correlationId correlation ID for tracing
     */
    public void sendRawLog(ServerConfiguration server, String logFilePath, String logLine,
                           String correlationId) {

        // Extract file name from path
        String fileName = Paths.get(logFilePath).getFileName().toString();

        // Determine log type from file extension
        String logType = extractLogType(fileName);

        // Parse timestamp from log line (simplified - just use current time)
        long originalTimestamp = System.currentTimeMillis();
        long collectionTimestamp = System.currentTimeMillis();

        // Build Avro message matching the schema
        RawLogMessage message = RawLogMessage.newBuilder()
                .setCorrelationId(correlationId)
                .setServerId(server.getId())
                .setServerHostname(server.getHostname())
                .setFileName(fileName)
                .setLogPath(logFilePath)
                .setLogType(logType)
                .setContent(logLine)
                .setOriginalTimestamp(originalTimestamp)
                .setCollectionTimestamp(collectionTimestamp)
                .build();

        // Use serverId as partition key for ordering
        String partitionKey = server.getId().toString();

        // Send asynchronously
        CompletableFuture<SendResult<String, RawLogMessage>> future =
                kafkaTemplate.send(rawLogsTopic, partitionKey, message);

        // Add callback for logging
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to send log message to Kafka: serverId={}, file={}, correlationId={}",
                        server.getId(), logFilePath, correlationId, ex);
            } else {
                log.trace("Sent log message to Kafka: partition={}, offset={}, correlationId={}",
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        correlationId);
            }
        });
    }

    /**
     * Sends a raw log synchronously (for testing or critical messages).
     *
     * @param server server configuration
     * @param logFilePath log file path
     * @param logLine log content
     * @param correlationId correlation ID for tracing
     * @return true if sent successfully
     */
    public boolean sendRawLogSync(ServerConfiguration server, String logFilePath, String logLine,
                                  String correlationId) {
        try {
            String fileName = Paths.get(logFilePath).getFileName().toString();
            String logType = extractLogType(fileName);
            long timestamp = System.currentTimeMillis();

            RawLogMessage message = RawLogMessage.newBuilder()
                    .setCorrelationId(correlationId)
                    .setServerId(server.getId())
                    .setServerHostname(server.getHostname())
                    .setFileName(fileName)
                    .setLogPath(logFilePath)
                    .setLogType(logType)
                    .setContent(logLine)
                    .setOriginalTimestamp(timestamp)
                    .setCollectionTimestamp(timestamp)
                    .build();

            String partitionKey = server.getId().toString();

            SendResult<String, RawLogMessage> result =
                    kafkaTemplate.send(rawLogsTopic, partitionKey, message).get();

            log.debug("Sent log message synchronously: partition={}, offset={}",
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());

            return true;

        } catch (Exception e) {
            log.error("Failed to send log message synchronously", e);
            return false;
        }
    }

    /**
     * Extracts log type from file name based on extension.
     *
     * @param fileName file name
     * @return log type identifier
     */
    private String extractLogType(String fileName) {
        if (fileName.endsWith(".log")) {
            return "application";
        } else if (fileName.contains("access")) {
            return "access";
        } else if (fileName.contains("error")) {
            return "error";
        } else if (fileName.contains("audit")) {
            return "audit";
        } else {
            return "unknown";
        }
    }
}
