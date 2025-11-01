package com.logcollector.domain.repository;

import com.logcollector.domain.model.Transaction;
import com.logcollector.domain.model.Transaction.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for Transaction entity.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    Page<Transaction> findByStatus(TransactionStatus status, Pageable pageable);

    Page<Transaction> findByStatusAndStartTimeBetween(
            TransactionStatus status,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Pageable pageable);

    Page<Transaction> findByStartTimeBetweenOrderByStartTimeDesc(
            LocalDateTime startTime,
            LocalDateTime endTime,
            Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.status = :status AND t.updatedAt < :before")
    List<Transaction> findStaleTransactions(
            @Param("status") TransactionStatus status,
            @Param("before") LocalDateTime before);

    @Query("SELECT t FROM Transaction t WHERE t.sourceSystem = :system AND t.startTime BETWEEN :start AND :end ORDER BY t.startTime DESC")
    List<Transaction> findBySourceSystemAndTimeRange(
            @Param("system") String system,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.status = :status AND t.startTime BETWEEN :start AND :end")
    long countByStatusInTimeRange(
            @Param("status") TransactionStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    void deleteByStartTimeBefore(LocalDateTime cutoffDate);
}
