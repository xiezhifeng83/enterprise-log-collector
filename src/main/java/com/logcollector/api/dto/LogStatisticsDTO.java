package com.logcollector.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for log statistics
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogStatisticsDTO {

    private Long totalLogs;

    private Long errorLogs;

    private Long parsedLogs;

    private Long unparsedLogs;

    private Double errorRate;
}
