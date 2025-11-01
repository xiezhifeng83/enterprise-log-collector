package com.logcollector.api.controller;

import com.logcollector.api.dto.LogEntryDTO;
import com.logcollector.api.dto.LogStatisticsDTO;
import com.logcollector.api.dto.PagedResponse;
import com.logcollector.api.service.LogQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST API for querying log entries
 */
@RestController
@RequestMapping("/api/v1/logs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Log Query", description = "APIs for querying and viewing log entries")
public class LogQueryController {

    private final LogQueryService logQueryService;

    /**
     * Query logs with filters and pagination
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    @Operation(summary = "Query logs", description = "Query logs with filters and pagination")
    public ResponseEntity<PagedResponse<LogEntryDTO>> queryLogs(
            @RequestParam(required = false) Long serverId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) Boolean errorOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Querying logs - serverId: {}, startTime: {}, endTime: {}, errorOnly: {}, page: {}, size: {}",
                serverId, startTime, endTime, errorOnly, page, size);

        PagedResponse<LogEntryDTO> response = logQueryService.queryLogs(
                serverId, startTime, endTime, errorOnly, page, size);

        return ResponseEntity.ok(response);
    }

    /**
     * Get log entry by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    @Operation(summary = "Get log by ID", description = "Get a specific log entry by its ID")
    public ResponseEntity<LogEntryDTO> getLogById(@PathVariable Long id) {
        log.info("Getting log entry with id: {}", id);
        LogEntryDTO logEntry = logQueryService.getLogById(id);
        return ResponseEntity.ok(logEntry);
    }

    /**
     * Get logs by transaction ID
     */
    @GetMapping("/transaction/{transactionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    @Operation(summary = "Get logs by transaction", description = "Get all logs associated with a transaction ID")
    public ResponseEntity<List<LogEntryDTO>> getLogsByTransaction(@PathVariable String transactionId) {
        log.info("Getting logs for transaction: {}", transactionId);
        List<LogEntryDTO> logs = logQueryService.getLogsByTransaction(transactionId);
        return ResponseEntity.ok(logs);
    }

    /**
     * Get error logs in time range
     */
    @GetMapping("/errors")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    @Operation(summary = "Get error logs", description = "Get all error logs in a time range")
    public ResponseEntity<List<LogEntryDTO>> getErrorLogs(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        log.info("Getting error logs from {} to {}", startTime, endTime);
        List<LogEntryDTO> errorLogs = logQueryService.getErrorLogs(startTime, endTime);
        return ResponseEntity.ok(errorLogs);
    }

    /**
     * Get log statistics
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    @Operation(summary = "Get log statistics", description = "Get statistics about logs in a time range")
    public ResponseEntity<LogStatisticsDTO> getStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        log.info("Getting log statistics from {} to {}", startTime, endTime);
        LogStatisticsDTO statistics = logQueryService.getStatistics(startTime, endTime);
        return ResponseEntity.ok(statistics);
    }
}
