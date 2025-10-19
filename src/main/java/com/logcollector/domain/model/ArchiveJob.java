package com.logcollector.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing a data archival operation.
 */
@Entity
@Table(name = "archive_jobs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArchiveJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "execution_time", nullable = false)
    private LocalDateTime executionTime;

    @Column(name = "start_date", nullable = false)
    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @Column(name = "record_count", nullable = false)
    private Long recordCount = 0L;

    @Column(name = "transaction_count", nullable = false)
    private Long transactionCount = 0L;

    @Column(name = "log_entry_count", nullable = false)
    private Long logEntryCount = 0L;

    @Column(name = "storage_location", nullable = false, length = 500)
    @NotBlank(message = "Storage location is required")
    private String storageLocation;

    @Column(name = "compression_ratio", precision = 5, scale = 2)
    private BigDecimal compressionRatio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ArchiveStatus status = ArchiveStatus.RUNNING;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "completion_time")
    private LocalDateTime completionTime;

    @PrePersist
    protected void onCreate() {
        if (executionTime == null) {
            executionTime = LocalDateTime.now();
        }
    }

    public enum ArchiveStatus {
        RUNNING,
        COMPLETED,
        FAILED
    }
}
