package com.logcollector.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing an individual log line collected from a server.
 *
 * Stored in partitioned table by month for efficient querying.
 */
@Entity
@Table(name = "log_entries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "server_id", nullable = false)
    @NotNull(message = "Server ID is required")
    private Long serverId;

    @Column(name = "file_name", nullable = false)
    @NotBlank(message = "File name is required")
    private String fileName;

    @Column(name = "log_path", nullable = false, length = 500)
    @NotBlank(message = "Log path is required")
    private String logPath;

    @Column(name = "log_type", nullable = false, length = 100)
    @NotBlank(message = "Log type is required")
    private String logType;

    @Column(nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "Content is required")
    private String content;

    @Column(name = "original_timestamp", nullable = false)
    @NotNull(message = "Original timestamp is required")
    private LocalDateTime originalTimestamp;

    @Column(name = "collection_timestamp", nullable = false)
    private LocalDateTime collectionTimestamp;

    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    @Column(nullable = false)
    private Boolean parsed = false;

    @Column(name = "error_indicator", nullable = false)
    private Boolean errorIndicator = false;

    @PrePersist
    protected void onCreate() {
        if (collectionTimestamp == null) {
            collectionTimestamp = LocalDateTime.now();
        }
    }
}
