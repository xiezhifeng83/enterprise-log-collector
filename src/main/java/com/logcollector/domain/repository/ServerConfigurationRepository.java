package com.logcollector.domain.repository;

import com.logcollector.domain.model.ServerConfiguration;
import com.logcollector.domain.model.ServerConfiguration.ServerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for ServerConfiguration entity.
 */
@Repository
public interface ServerConfigurationRepository extends JpaRepository<ServerConfiguration, Long> {

    Optional<ServerConfiguration> findByHostname(String hostname);

    List<ServerConfiguration> findByStatus(ServerStatus status);

    List<ServerConfiguration> findByStatusAndLastCollectionAtBefore(
            ServerStatus status, LocalDateTime before);

    @Query("SELECT s FROM ServerConfiguration s WHERE s.status = 'CONNECTED' ORDER BY s.lastCollectionAt ASC")
    List<ServerConfiguration> findConnectedServersOrderedByLastCollection();

    boolean existsByHostname(String hostname);
}
