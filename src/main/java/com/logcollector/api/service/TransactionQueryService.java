package com.logcollector.api.service;

import com.logcollector.api.dto.LogEntryDTO;
import com.logcollector.api.dto.PagedResponse;
import com.logcollector.api.dto.TransactionDTO;
import com.logcollector.domain.model.Transaction;
import com.logcollector.domain.model.Transaction.TransactionStatus;
import com.logcollector.domain.repository.LogEntryRepository;
import com.logcollector.domain.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for querying transactions
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TransactionQueryService {

    private final TransactionRepository transactionRepository;
    private final LogEntryRepository logEntryRepository;

    /**
     * Query transactions with pagination
     */
    public PagedResponse<TransactionDTO> queryTransactions(
            String status,
            LocalDateTime startTime,
            LocalDateTime endTime,
            int page,
            int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startTime"));

        Page<Transaction> transactionPage;

        if (status != null && startTime != null && endTime != null) {
            TransactionStatus transactionStatus = TransactionStatus.valueOf(status);
            transactionPage = transactionRepository.findByStatusAndStartTimeBetween(
                    transactionStatus, startTime, endTime, pageable);
        } else if (startTime != null && endTime != null) {
            transactionPage = transactionRepository.findByStartTimeBetweenOrderByStartTimeDesc(
                    startTime, endTime, pageable);
        } else if (status != null) {
            TransactionStatus transactionStatus = TransactionStatus.valueOf(status);
            transactionPage = transactionRepository.findByStatus(transactionStatus, pageable);
        } else {
            transactionPage = transactionRepository.findAll(pageable);
        }

        List<TransactionDTO> content = transactionPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return PagedResponse.of(
                content,
                transactionPage.getTotalElements(),
                transactionPage.getTotalPages(),
                transactionPage.getNumber(),
                transactionPage.getSize()
        );
    }

    /**
     * Get transaction by ID
     */
    public TransactionDTO getTransactionById(String transactionId) {
        return transactionRepository.findById(transactionId)
                .map(this::convertToDTO)
                .orElseThrow(() -> new RuntimeException("Transaction not found with id: " + transactionId));
    }

    /**
     * Get logs for a specific transaction
     */
    public List<LogEntryDTO> getTransactionLogs(String transactionId) {
        return logEntryRepository.findByTransactionIdOrderByOriginalTimestampAsc(transactionId)
                .stream()
                .map(logEntry -> LogEntryDTO.builder()
                        .id(logEntry.getId())
                        .serverId(logEntry.getServerId())
                        .fileName(logEntry.getFileName())
                        .logPath(logEntry.getLogPath())
                        .logType(logEntry.getLogType())
                        .content(logEntry.getContent())
                        .originalTimestamp(logEntry.getOriginalTimestamp())
                        .collectionTimestamp(logEntry.getCollectionTimestamp())
                        .transactionId(logEntry.getTransactionId())
                        .parsed(logEntry.getParsed())
                        .errorIndicator(logEntry.getErrorIndicator())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Convert Transaction entity to DTO
     */
    private TransactionDTO convertToDTO(Transaction transaction) {
        return TransactionDTO.builder()
                .transactionId(transaction.getTransactionId())
                .status(transaction.getStatus().name())
                .startTime(transaction.getStartTime())
                .endTime(transaction.getEndTime())
                .durationMs(transaction.getDurationMs())
                .sourceSystem(transaction.getSourceSystem())
                .targetSystem(transaction.getTargetSystem())
                .errorMessage(transaction.getErrorMessage())
                .retryCount(transaction.getRetryCount())
                .build();
    }
}
