package com.logcollector.domain.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Entity representing a single step in a transaction's journey across systems.
 */
@Entity
@Table(name = "transaction_flow_nodes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionFlowNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false, length = 100)
    @NotBlank(message = "Transaction ID is required")
    private String transactionId;

    @Column(name = "system_name", nullable = false, length = 100)
    @NotBlank(message = "System name is required")
    private String systemName;

    @Column(name = "step_name", nullable = false, length = 200)
    @NotBlank(message = "Step name is required")
    private String stepName;

    @Column(nullable = false)
    @NotNull(message = "Timestamp is required")
    private LocalDateTime timestamp;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FlowNodeStatus status;

    @Column(name = "sequence_order", nullable = false)
    @NotNull(message = "Sequence order is required")
    private Integer sequenceOrder;

    @Column(columnDefinition = "JSON")
    @Convert(converter = MetadataConverter.class)
    private Map<String, String> metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum FlowNodeStatus {
        STARTED,
        COMPLETED,
        FAILED
    }

    /**
     * JPA converter for metadata map.
     */
    @Converter
    public static class MetadataConverter implements AttributeConverter<Map<String, String>, String> {
        private static final ObjectMapper objectMapper = new ObjectMapper();

        @Override
        public String convertToDatabaseColumn(Map<String, String> attribute) {
            if (attribute == null || attribute.isEmpty()) {
                return "{}";
            }
            try {
                return objectMapper.writeValueAsString(attribute);
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Error converting map to JSON", e);
            }
        }

        @Override
        public Map<String, String> convertToEntityAttribute(String dbData) {
            if (dbData == null || dbData.isEmpty()) {
                return new HashMap<>();
            }
            try {
                return objectMapper.readValue(dbData, new TypeReference<Map<String, String>>() {});
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Error converting JSON to map", e);
            }
        }
    }
}
