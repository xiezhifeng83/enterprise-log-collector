package com.logcollector.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Entity representing an alert rule configuration.
 */
@Entity
@Table(name = "alert_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 200)
    @NotBlank(message = "Rule name is required")
    private String name;

    @Column(name = "alert_type", nullable = false, length = 100)
    @NotBlank(message = "Alert type is required")
    private String alertType;

    @Column(nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "Condition is required")
    private String condition;

    @Column(precision = 10, scale = 4)
    private BigDecimal threshold;

    @Column(name = "time_window_minutes", nullable = false)
    @Min(value = 1, message = "Time window must be at least 1 minute")
    @Max(value = 60, message = "Time window must not exceed 60 minutes")
    private Integer timeWindowMinutes = 5;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertSeverity severity;

    @Column(name = "delivery_channels", nullable = false, columnDefinition = "JSON")
    @Convert(converter = StringListConverter.class)
    @NotEmpty(message = "At least one delivery channel is required")
    private List<String> deliveryChannels;

    @Column(name = "email_recipients", columnDefinition = "JSON")
    @Convert(converter = StringListConverter.class)
    private List<String> emailRecipients;

    @Column(name = "webhook_url", length = 500)
    private String webhookUrl;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "created_by", nullable = false)
    @NotNull(message = "Created by user ID is required")
    private Long createdBy;

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

    public enum AlertSeverity {
        CRITICAL,
        WARNING,
        INFO
    }

    public enum AlertType {
        ERROR_RATE,
        TRANSACTION_TIMEOUT,
        SYSTEM_DOWN,
        PATTERN_MATCH
    }
}
