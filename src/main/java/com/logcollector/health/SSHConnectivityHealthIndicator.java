package com.logcollector.health;

import com.logcollector.domain.model.ServerConfiguration;
import com.logcollector.domain.repository.ServerConfigurationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Health indicator for SSH connectivity to configured servers.
 *
 * Reports overall health based on server connection status.
 */
@Component("sshConnectivity")
@Slf4j
@RequiredArgsConstructor
public class SSHConnectivityHealthIndicator implements HealthIndicator {

    private final ServerConfigurationRepository serverRepository;

    @Override
    public Health health() {
        try {
            List<ServerConfiguration> servers = serverRepository.findAll();

            if (servers.isEmpty()) {
                return Health.up()
                        .withDetail("message", "No servers configured")
                        .withDetail("totalServers", 0)
                        .build();
            }

            // Count servers by status
            long connectedCount = servers.stream()
                    .filter(s -> s.getStatus() == ServerConfiguration.ServerStatus.CONNECTED)
                    .count();

            long disconnectedCount = servers.stream()
                    .filter(s -> s.getStatus() == ServerConfiguration.ServerStatus.DISCONNECTED)
                    .count();

            long errorCount = servers.stream()
                    .filter(s -> s.getStatus() == ServerConfiguration.ServerStatus.ERROR)
                    .count();

            // Build details
            Map<String, Object> details = new HashMap<>();
            details.put("totalServers", servers.size());
            details.put("connectedServers", connectedCount);
            details.put("disconnectedServers", disconnectedCount);
            details.put("errorServers", errorCount);
            details.put("connectionRate", String.format("%.1f%%",
                    (connectedCount * 100.0) / servers.size()));

            // Add error details if any servers have errors
            if (errorCount > 0) {
                List<String> errorDetails = servers.stream()
                        .filter(s -> s.getStatus() == ServerConfiguration.ServerStatus.ERROR)
                        .map(s -> String.format("%s: %s",
                                s.getHostname(),
                                s.getLastErrorMessage() != null ? s.getLastErrorMessage() : "Unknown error"))
                        .toList();

                details.put("errors", errorDetails);
            }

            // Determine overall health status
            // DOWN if more than 50% of servers are in ERROR state
            // UP if at least one server is connected
            // UNKNOWN if all servers are disconnected
            if (errorCount > servers.size() / 2) {
                return Health.down()
                        .withDetails(details)
                        .build();
            } else if (connectedCount > 0) {
                return Health.up()
                        .withDetails(details)
                        .build();
            } else {
                return Health.unknown()
                        .withDetails(details)
                        .build();
            }

        } catch (Exception e) {
            log.error("Failed to check SSH connectivity health", e);
            return Health.down()
                    .withException(e)
                    .build();
        }
    }
}
