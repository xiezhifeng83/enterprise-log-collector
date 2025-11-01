package com.logcollector.api.dto;

import com.logcollector.domain.model.ServerConfiguration;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for server configuration in API responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerConfigurationDTO {

    private Long id;

    @NotBlank(message = "Hostname is required")
    @Size(max = 255, message = "Hostname must be less than 255 characters")
    private String hostname;

    @Min(value = 1, message = "Port must be between 1 and 65535")
    @Max(value = 65535, message = "Port must be between 1 and 65535")
    private Integer port;

    @NotBlank(message = "Username is required")
    @Size(max = 100, message = "Username must be less than 100 characters")
    private String username;

    // Password only accepted in create/update requests, never returned
    @Size(min = 1, message = "Password is required")
    private String password;

    @NotEmpty(message = "At least one log file path is required")
    private List<String> logFilePaths;

    @Min(value = 5000, message = "Collection interval must be at least 5 seconds (5000ms)")
    @Max(value = 3600000, message = "Collection interval must be at most 1 hour (3600000ms)")
    private Integer collectionInterval;

    @Min(value = 10, message = "Batch size must be at least 10")
    @Max(value = 10000, message = "Batch size must be at most 10000")
    private Integer batchSize;

    private String status;
    private String lastErrorMessage;
    private LocalDateTime lastCollectionAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Converts entity to DTO (excludes sensitive data like password).
     */
    public static ServerConfigurationDTO fromEntity(ServerConfiguration entity) {
        return ServerConfigurationDTO.builder()
                .id(entity.getId())
                .hostname(entity.getHostname())
                .port(entity.getPort())
                .username(entity.getUsername())
                // Note: password is never included in responses
                .logFilePaths(entity.getLogFilePaths())
                .collectionInterval(entity.getCollectionInterval())
                .batchSize(entity.getBatchSize())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .lastErrorMessage(entity.getLastErrorMessage())
                .lastCollectionAt(entity.getLastCollectionAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * Converts DTO to entity for create/update operations.
     * Note: Password will be encrypted by the service layer.
     */
    public ServerConfiguration toEntity() {
        return ServerConfiguration.builder()
                .id(this.id)
                .hostname(this.hostname)
                .port(this.port != null ? this.port : 22) // Default SSH port
                .username(this.username)
                .encryptedPassword(this.password) // Will be encrypted by service
                .logFilePaths(this.logFilePaths)
                .collectionInterval(this.collectionInterval != null ? this.collectionInterval : 30000)
                .batchSize(this.batchSize != null ? this.batchSize : 500)
                .status(ServerConfiguration.ServerStatus.DISCONNECTED)
                .build();
    }
}
