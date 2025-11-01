package com.logcollector.integration.database;

import com.logcollector.domain.model.ServerConfiguration;
import com.logcollector.domain.repository.ServerConfigurationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for server configuration CRUD operations.
 *
 * Uses Testcontainers MySQL for real database testing.
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ServerConfigurationRepositoryTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private ServerConfigurationRepository repository;

    @Test
    void testCreateServerConfiguration() {
        // Given
        ServerConfiguration server = ServerConfiguration.builder()
                .hostname("test-server-1")
                .port(22)
                .username("testuser")
                .encryptedPassword("encrypted_password")
                .logFilePaths(Arrays.asList("/var/log/app.log"))
                .collectionInterval(30000)
                .batchSize(500)
                .status(ServerConfiguration.ServerStatus.DISCONNECTED)
                .build();

        // When
        ServerConfiguration saved = repository.save(server);

        // Then
        assertNotNull(saved.getId());
        assertEquals("test-server-1", saved.getHostname());
    }

    @Test
    void testFindByHostname() {
        // Given
        ServerConfiguration server = ServerConfiguration.builder()
                .hostname("test-server-2")
                .port(22)
                .username("testuser")
                .encryptedPassword("encrypted_password")
                .logFilePaths(Arrays.asList("/var/log/app.log"))
                .collectionInterval(30000)
                .batchSize(500)
                .build();
        repository.save(server);

        // When
        Optional<ServerConfiguration> found = repository.findByHostname("test-server-2");

        // Then
        assertTrue(found.isPresent());
        assertEquals("test-server-2", found.get().getHostname());
    }

    @Test
    void testFindByStatus() {
        // Test finding servers by connection status
        // This will fail until we have proper test data
    }

    @Test
    void testUpdateServerStatus() {
        // Test updating server status from DISCONNECTED to CONNECTED
        // This will fail until implementation
    }
}
