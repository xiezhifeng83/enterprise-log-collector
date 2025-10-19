package com.logcollector.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Entity representing a remote Linux server configuration for log collection.
 *
 * Stores connection details, collection settings, and status tracking.
 */
@Entity
@Table(name = "server_configurations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServerConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    @NotBlank(message = "Hostname is required")
    @Size(max = 255, message = "Hostname must not exceed 255 characters")
    private String hostname;

    @Column(nullable = false)
    @Min(value = 1, message = "Port must be between 1 and 65535")
    @Max(value = 65535, message = "Port must be between 1 and 65535")
    private Integer port = 22;

    @Column(nullable = false, length = 100)
    @NotBlank(message = "Username is required")
    private String username;

    @Column(name = "encrypted_password", nullable = false, length = 512)
    @NotBlank(message = "Password is required")
    private String encryptedPassword;

    @Column(name = "log_file_paths", nullable = false, columnDefinition = "JSON")
    @Convert(converter = StringListConverter.class)
    @NotEmpty(message = "At least one log file path is required")
    private List<String> logFilePaths;

    @Column(name = "collection_interval", nullable = false)
    @Min(value = 10000, message = "Collection interval must be at least 10 seconds")
    private Integer collectionInterval = 30000;

    @Column(name = "batch_size", nullable = false)
    @Min(value = 100, message = "Batch size must be at least 100")
    @Max(value = 10000, message = "Batch size must not exceed 10000")
    private Integer batchSize = 500;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServerStatus status = ServerStatus.DISCONNECTED;

    @Column(name = "last_connected_at")
    private LocalDateTime lastConnectedAt;

    @Column(name = "last_collection_at")
    private LocalDateTime lastCollectionAt;

    @Column(name = "last_error_message", columnDefinition = "TEXT")
    private String lastErrorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum ServerStatus {
        CONNECTED,
        DISCONNECTED,
        ERROR
    }
}
