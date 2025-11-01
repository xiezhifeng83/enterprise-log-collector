package com.logcollector.api.controller;

import com.logcollector.api.dto.ServerConfigurationDTO;
import com.logcollector.api.service.ServerConfigurationService;
import com.logcollector.domain.model.ServerConfiguration;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for server configuration management.
 *
 * Provides endpoints for CRUD operations on server configurations.
 */
@RestController
@RequestMapping("/v1/servers")
@Slf4j
@RequiredArgsConstructor
public class ServerController {

    private final ServerConfigurationService serverConfigurationService;

    /**
     * Creates a new server configuration.
     *
     * @param dto server configuration data
     * @return created server configuration
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServerConfigurationDTO> createServer(@Valid @RequestBody ServerConfigurationDTO dto) {
        log.info("Creating new server configuration for hostname: {}", dto.getHostname());

        ServerConfiguration created = serverConfigurationService.createServer(dto);
        ServerConfigurationDTO responseDto = ServerConfigurationDTO.fromEntity(created);

        log.info("Created server configuration with ID: {}", created.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    /**
     * Gets all server configurations.
     *
     * @return list of server configurations
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<List<ServerConfigurationDTO>> getAllServers() {
        log.debug("Fetching all server configurations");

        List<ServerConfiguration> servers = serverConfigurationService.getAllServers();
        List<ServerConfigurationDTO> dtos = servers.stream()
                .map(ServerConfigurationDTO::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    /**
     * Gets a server configuration by ID.
     *
     * @param id server ID
     * @return server configuration
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ServerConfigurationDTO> getServerById(@PathVariable Long id) {
        log.debug("Fetching server configuration with ID: {}", id);

        ServerConfiguration server = serverConfigurationService.getServerById(id);
        ServerConfigurationDTO dto = ServerConfigurationDTO.fromEntity(server);

        return ResponseEntity.ok(dto);
    }

    /**
     * Updates an existing server configuration.
     *
     * @param id server ID
     * @param dto updated server configuration data
     * @return updated server configuration
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServerConfigurationDTO> updateServer(
            @PathVariable Long id,
            @Valid @RequestBody ServerConfigurationDTO dto) {

        log.info("Updating server configuration with ID: {}", id);

        ServerConfiguration updated = serverConfigurationService.updateServer(id, dto);
        ServerConfigurationDTO responseDto = ServerConfigurationDTO.fromEntity(updated);

        return ResponseEntity.ok(responseDto);
    }

    /**
     * Deletes a server configuration.
     *
     * @param id server ID
     * @return no content
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteServer(@PathVariable Long id) {
        log.info("Deleting server configuration with ID: {}", id);

        serverConfigurationService.deleteServer(id);

        return ResponseEntity.noContent().build();
    }

    /**
     * Tests SSH connection to a server without saving configuration.
     *
     * @param dto server configuration to test
     * @return connection test result
     */
    @PostMapping("/test-connection")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<ConnectionTestResult> testConnection(@Valid @RequestBody ServerConfigurationDTO dto) {
        log.info("Testing SSH connection to hostname: {}", dto.getHostname());

        boolean success = serverConfigurationService.testConnection(dto);

        ConnectionTestResult result = new ConnectionTestResult(
                success,
                success ? "Connection successful" : "Connection failed"
        );

        return ResponseEntity.ok(result);
    }

    /**
     * Gets servers by status.
     *
     * @param status server status (CONNECTED, DISCONNECTED, ERROR)
     * @return list of servers with the specified status
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<List<ServerConfigurationDTO>> getServersByStatus(@PathVariable String status) {
        log.debug("Fetching servers with status: {}", status);

        List<ServerConfiguration> servers = serverConfigurationService.getServersByStatus(
                ServerConfiguration.ServerStatus.valueOf(status.toUpperCase())
        );

        List<ServerConfigurationDTO> dtos = servers.stream()
                .map(ServerConfigurationDTO::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    /**
     * DTO for connection test results.
     */
    public record ConnectionTestResult(boolean success, String message) {}
}
