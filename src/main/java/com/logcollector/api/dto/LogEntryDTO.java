package com.logcollector.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for log entry responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogEntryDTO {

    private Long id;

    private Long serverId;

    private String fileName;

    private String logPath;

    private String logType;

    private String content;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime originalTimestamp;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime collectionTimestamp;

    private String transactionId;

    private Boolean parsed;

    private Boolean errorIndicator;
}
