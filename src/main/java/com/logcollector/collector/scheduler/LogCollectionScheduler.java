package com.logcollector.collector.scheduler;

import com.logcollector.collector.service.LogCollectionService;
import com.logcollector.domain.model.ServerConfiguration;
import com.logcollector.domain.repository.ServerConfigurationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Schedules periodic log collection from all configured servers.
 *
 * Runs collection tasks in parallel for all active servers.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class LogCollectionScheduler {

    private final ServerConfigurationRepository serverRepository;
    private final LogCollectionService logCollectionService;

    // Thread pool for parallel collection (sized based on expected server count)
    private final ExecutorService collectionExecutor = Executors.newFixedThreadPool(10);

    /**
     * Scheduled task that collects logs from all connected servers.
     *
     * Runs every 30 seconds by default (configurable per server).
     * Each server collection runs in parallel.
     */
    @Scheduled(fixedDelayString = "${log-collection.scheduler.interval:30000}")
    public void collectFromAllServers() {
        log.info("Starting scheduled log collection cycle");

        try {
            // Get all servers that are ready for collection
            List<ServerConfiguration> servers = serverRepository
                    .findConnectedServersOrderedByLastCollection();

            if (servers.isEmpty()) {
                log.debug("No servers configured for collection");
                return;
            }

            log.info("Found {} servers ready for log collection", servers.size());

            // Launch collection tasks in parallel
            List<CompletableFuture<Void>> collectionTasks = servers.stream()
                    .map(server -> CompletableFuture.runAsync(
                            () -> collectFromServer(server),
                            collectionExecutor
                    ))
                    .toList();

            // Wait for all collection tasks to complete
            CompletableFuture.allOf(collectionTasks.toArray(new CompletableFuture[0]))
                    .join();

            log.info("Completed log collection cycle for {} servers", servers.size());

        } catch (Exception e) {
            log.error("Error during scheduled log collection cycle", e);
        }
    }

    /**
     * Collects logs from a single server.
     *
     * @param server server configuration
     */
    private void collectFromServer(ServerConfiguration server) {
        try {
            // Check if enough time has passed since last collection
            if (!shouldCollectNow(server)) {
                log.debug("Skipping collection for {} - collection interval not reached",
                        server.getHostname());
                return;
            }

            log.info("Collecting logs from server: {}", server.getHostname());

            // Delegate to collection service
            logCollectionService.collectLogsFromServer(server);

        } catch (Exception e) {
            log.error("Failed to collect logs from server {}", server.getHostname(), e);
            // Error handling is done in LogCollectionService
        }
    }

    /**
     * Checks if enough time has passed since last collection.
     *
     * @param server server configuration
     * @return true if collection should run now
     */
    private boolean shouldCollectNow(ServerConfiguration server) {
        if (server.getLastCollectionAt() == null) {
            return true; // Never collected before
        }

        Duration timeSinceLastCollection = Duration.between(
                server.getLastCollectionAt(),
                LocalDateTime.now()
        );

        return timeSinceLastCollection.toMillis() >= server.getCollectionInterval();
    }

    /**
     * Shutdown hook to clean up executor service.
     */
    public void shutdown() {
        log.info("Shutting down log collection scheduler");
        collectionExecutor.shutdown();
    }
}
