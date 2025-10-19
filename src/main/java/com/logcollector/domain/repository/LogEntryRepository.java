package com.logcollector.domain.repository;

import com.logcollector.domain.model.LogEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for LogEntry entity.
 */
@Repository
public interface LogEntryRepository extends JpaRepository<LogEntry, Long> {

    Page<LogEntry> findByServerIdAndOriginalTimestampBetween(
            Long serverId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Pageable pageable);

    List<LogEntry> findByTransactionIdOrderByOriginalTimestampAsc(String transactionId);

    @Query("SELECT l FROM LogEntry l WHERE l.parsed = false AND l.collectionTimestamp < :before ORDER BY l.collectionTimestamp ASC")
    List<LogEntry> findUnparsedEntriesBefore(@Param("before") LocalDateTime before, Pageable pageable);

    @Query("SELECT l FROM LogEntry l WHERE l.errorIndicator = true AND l.originalTimestamp BETWEEN :start AND :end")
    List<LogEntry> findErrorEntriesInTimeRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(l) FROM LogEntry l WHERE l.serverId = :serverId AND l.originalTimestamp BETWEEN :start AND :end")
    long countByServerAndTimeRange(
            @Param("serverId") Long serverId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(l) FROM LogEntry l WHERE l.errorIndicator = true AND l.originalTimestamp BETWEEN :start AND :end")
    long countErrorsInTimeRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    void deleteByOriginalTimestampBefore(LocalDateTime cutoffDate);
}
