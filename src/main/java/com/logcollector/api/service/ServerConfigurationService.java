package com.logcollector.api.service;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.logcollector.api.dto.ServerConfigurationDTO;
import com.logcollector.collector.ssh.SSHConnectionManager;
import com.logcollector.config.security.EncryptionUtil;
import com.logcollector.domain.model.ServerConfiguration;
import com.logcollector.domain.repository.ServerConfigurationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for managing server configurations.
 *
 * Handles CRUD operations, password encryption, and connection testing.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ServerConfigurationService {

    private final ServerConfigurationRepository serverRepository;
    private final EncryptionUtil encryptionUtil;
    private final SSHConnectionManager sshConnectionManager;

    /**
     * Creates a new server configuration.
     *
     * @param dto server configuration data
     * @return created server configuration
     */
    @Transactional
    public ServerConfiguration createServer(ServerConfigurationDTO dto) {
        log.info("Creating server configuration for hostname: {}", dto.getHostname());

        // Check if server with same hostname already exists
        serverRepository.findByHostname(dto.getHostname()).ifPresent(existing -> {
            throw new ServerAlreadyExistsException(
                    "Server with hostname " + dto.getHostname() + " already exists"
            );
        });

        // Convert DTO to entity
        ServerConfiguration server = dto.toEntity();

        // Encrypt password
        String encryptedPassword = encryptionUtil.encrypt(dto.getPassword());
        server.setEncryptedPassword(encryptedPassword);

        // Save server
        ServerConfiguration saved = serverRepository.save(server);

        log.info("Created server configuration with ID: {}", saved.getId());
        return saved;
    }

    /**
     * Gets all server configurations.
     *
     * @return list of all servers
     */
    @Transactional(readOnly = true)
    public List<ServerConfiguration> getAllServers() {
        return serverRepository.findAll();
    }

    /**
     * Gets a server configuration by ID.
     *
     * @param id server ID
     * @return server configuration
     * @throws ServerNotFoundException if server not found
     */
    @Transactional(readOnly = true)
    public ServerConfiguration getServerById(Long id) {
        return serverRepository.findById(id)
                .orElseThrow(() -> new ServerNotFoundException("Server not found with ID: " + id));
    }

    /**
     * Updates an existing server configuration.
     *
     * @param id server ID
     * @param dto updated server configuration data
     * @return updated server configuration
     */
    @Transactional
    public ServerConfiguration updateServer(Long id, ServerConfigurationDTO dto) {
        log.info("Updating server configuration with ID: {}", id);

        ServerConfiguration existing = getServerById(id);

        // Update fields
        existing.setHostname(dto.getHostname());
        existing.setPort(dto.getPort());
        existing.setUsername(dto.getUsername());

        // Update password if provided
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            String encryptedPassword = encryptionUtil.encrypt(dto.getPassword());
            existing.setEncryptedPassword(encryptedPassword);
        }

        existing.setLogFilePaths(dto.getLogFilePaths());
        existing.setCollectionInterval(dto.getCollectionInterval());
        existing.setBatchSize(dto.getBatchSize());

        // Reset status to DISCONNECTED when configuration changes
        existing.setStatus(ServerConfiguration.ServerStatus.DISCONNECTED);
        existing.setLastErrorMessage(null);

        ServerConfiguration updated = serverRepository.save(existing);

        log.info("Updated server configuration with ID: {}", id);
        return updated;
    }

    /**
     * Deletes a server configuration.
     *
     * @param id server ID
     */
    @Transactional
    public void deleteServer(Long id) {
        log.info("Deleting server configuration with ID: {}", id);

        ServerConfiguration server = getServerById(id);

        // Disconnect SSH session if connected
        sshConnectionManager.disconnect(
                server.getHostname(),
                server.getPort(),
                server.getUsername()
        );

        serverRepository.delete(server);

        log.info("Deleted server configuration with ID: {}", id);
    }

    /**
     * Tests SSH connection to a server without saving configuration.
     *
     * @param dto server configuration to test
     * @return true if connection successful
     */
    public boolean testConnection(ServerConfigurationDTO dto) {
        log.info("Testing SSH connection to hostname: {}", dto.getHostname());

        try {
            Session session = sshConnectionManager.connect(
                    dto.getHostname(),
                    dto.getPort() != null ? dto.getPort() : 22,
                    dto.getUsername(),
                    dto.getPassword()
            );

            boolean connected = session != null && session.isConnected();

            // Disconnect test session
            if (connected) {
                sshConnectionManager.disconnect(
                        dto.getHostname(),
                        dto.getPort() != null ? dto.getPort() : 22,
                        dto.getUsername()
                );
            }

            log.info("SSH connection test {} for hostname: {}",
                    connected ? "succeeded" : "failed", dto.getHostname());

            return connected;

        } catch (JSchException e) {
            log.warn("SSH connection test failed for hostname: {}", dto.getHostname(), e);
            return false;
        }
    }

    /**
     * Gets servers by status.
     *
     * @param status server status
     * @return list of servers with the specified status
     */
    @Transactional(readOnly = true)
    public List<ServerConfiguration> getServersByStatus(ServerConfiguration.ServerStatus status) {
        return serverRepository.findByStatus(status);
    }

    /**
     * Exception thrown when server already exists.
     */
    public static class ServerAlreadyExistsException extends RuntimeException {
        public ServerAlreadyExistsException(String message) {
            super(message);
        }
    }

    /**
     * Exception thrown when server not found.
     */
    public static class ServerNotFoundException extends RuntimeException {
        public ServerNotFoundException(String message) {
            super(message);
        }
    }
}
