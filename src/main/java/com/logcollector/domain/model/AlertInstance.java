package com.logcollector.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Entity representing a specific occurrence of an alert being triggered.
 */
@Entity
@Table(name = "alert_instances")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_id", nullable = false)
    @NotNull(message = "Rule ID is required")
    private Long ruleId;

    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertRule.AlertSeverity severity;

    @Column(nullable = false, length = 300)
    @NotBlank(message = "Title is required")
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "Message is required")
    private String message;

    @Column(name = "alert_time", nullable = false)
    private LocalDateTime alertTime;

    @Column(name = "resolved_time")
    private LocalDateTime resolvedTime;

    @Column(nullable = false)
    private Boolean resolved = false;

    @Column(nullable = false)
    private Boolean acknowledged = false;

    @Column(name = "acknowledged_by")
    private Long acknowledgedBy;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "notification_status", nullable = false, columnDefinition = "JSON")
    @Convert(converter = TransactionFlowNode.MetadataConverter.class)
    private Map<String, String> notificationStatus;

    @PrePersist
    protected void onCreate() {
        if (alertTime == null) {
            alertTime = LocalDateTime.now();
        }
    }
}
