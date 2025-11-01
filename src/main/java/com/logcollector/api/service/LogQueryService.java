package com.logcollector.api.service;

import com.logcollector.api.dto.LogEntryDTO;
import com.logcollector.api.dto.LogStatisticsDTO;
import com.logcollector.api.dto.PagedResponse;
import com.logcollector.domain.model.LogEntry;
import com.logcollector.domain.repository.LogEntryRepository;
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
 * Service for querying log entries
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class LogQueryService {

    private final LogEntryRepository logEntryRepository;

    /**
     * Query logs with pagination
     */
    public PagedResponse<LogEntryDTO> queryLogs(
            Long serverId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Boolean errorOnly,
            int page,
            int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "originalTimestamp"));

        Page<LogEntry> logPage;

        if (serverId != null && startTime != null && endTime != null) {
            logPage = logEntryRepository.findByServerIdAndOriginalTimestampBetween(
                    serverId, startTime, endTime, pageable);
        } else {
            logPage = logEntryRepository.findAll(pageable);
        }

        // Filter by error indicator if requested
        List<LogEntryDTO> content = logPage.getContent().stream()
                .filter(log -> errorOnly == null || !errorOnly || log.getErrorIndicator())
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return PagedResponse.of(
                content,
                logPage.getTotalElements(),
                logPage.getTotalPages(),
                logPage.getNumber(),
                logPage.getSize()
        );
    }

    /**
     * Get log entry by ID
     */
    public LogEntryDTO getLogById(Long id) {
        return logEntryRepository.findById(id)
                .map(this::convertToDTO)
                .orElseThrow(() -> new RuntimeException("Log entry not found with id: " + id));
    }

    /**
     * Get logs by transaction ID
     */
    public List<LogEntryDTO> getLogsByTransaction(String transactionId) {
        return logEntryRepository.findByTransactionIdOrderByOriginalTimestampAsc(transactionId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get error logs in time range
     */
    public List<LogEntryDTO> getErrorLogs(LocalDateTime startTime, LocalDateTime endTime) {
        return logEntryRepository.findErrorEntriesInTimeRange(startTime, endTime)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get log statistics
     */
    public LogStatisticsDTO getStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        long totalLogs = logEntryRepository.countAllInTimeRange(startTime, endTime);
        long errorLogs = logEntryRepository.countErrorsInTimeRange(startTime, endTime);

        // Get parsed/unparsed counts - only count logs in the time range
        List<LogEntry> allLogs = logEntryRepository.findAll();
        long parsedLogs = allLogs.stream()
                .filter(log -> !log.getOriginalTimestamp().isBefore(startTime) &&
                               !log.getOriginalTimestamp().isAfter(endTime))
                .filter(LogEntry::getParsed)
                .count();
        long unparsedLogs = totalLogs - parsedLogs;

        double errorRate = totalLogs > 0 ? (double) errorLogs / totalLogs : 0.0;

        return LogStatisticsDTO.builder()
                .totalLogs(totalLogs)
                .errorLogs(errorLogs)
                .parsedLogs(parsedLogs)
                .unparsedLogs(unparsedLogs)
                .errorRate(errorRate)
                .build();
    }

    /**
     * Convert LogEntry entity to DTO
     */
    private LogEntryDTO convertToDTO(LogEntry logEntry) {
        return LogEntryDTO.builder()
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
                .build();
    }
}
