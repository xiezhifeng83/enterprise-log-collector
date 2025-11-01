package com.logcollector.api.controller;

import com.logcollector.api.dto.LogEntryDTO;
import com.logcollector.api.dto.PagedResponse;
import com.logcollector.api.dto.TransactionDTO;
import com.logcollector.api.service.TransactionQueryService;
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
 * REST API for querying transactions
 */
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Transaction Query", description = "APIs for querying transactions and related logs")
public class TransactionController {

    private final TransactionQueryService transactionQueryService;

    /**
     * Query transactions with filters and pagination
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    @Operation(summary = "Query transactions", description = "Query transactions with filters and pagination")
    public ResponseEntity<PagedResponse<TransactionDTO>> queryTransactions(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Querying transactions - status: {}, startTime: {}, endTime: {}, page: {}, size: {}",
                status, startTime, endTime, page, size);

        PagedResponse<TransactionDTO> response = transactionQueryService.queryTransactions(
                status, startTime, endTime, page, size);

        return ResponseEntity.ok(response);
    }

    /**
     * Get transaction by ID
     */
    @GetMapping("/{transactionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    @Operation(summary = "Get transaction by ID", description = "Get a specific transaction by its ID")
    public ResponseEntity<TransactionDTO> getTransactionById(@PathVariable String transactionId) {
        log.info("Getting transaction with id: {}", transactionId);
        TransactionDTO transaction = transactionQueryService.getTransactionById(transactionId);
        return ResponseEntity.ok(transaction);
    }

    /**
     * Get all logs for a transaction
     */
    @GetMapping("/{transactionId}/logs")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    @Operation(summary = "Get transaction logs", description = "Get all logs associated with a transaction")
    public ResponseEntity<List<LogEntryDTO>> getTransactionLogs(@PathVariable String transactionId) {
        log.info("Getting logs for transaction: {}", transactionId);
        List<LogEntryDTO> logs = transactionQueryService.getTransactionLogs(transactionId);
        return ResponseEntity.ok(logs);
    }
}
