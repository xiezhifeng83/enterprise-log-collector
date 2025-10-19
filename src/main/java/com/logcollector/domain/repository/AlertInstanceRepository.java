package com.logcollector.domain.repository;

import com.logcollector.domain.model.AlertInstance;
import com.logcollector.domain.model.AlertRule.AlertSeverity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for AlertInstance entity.
 */
@Repository
public interface AlertInstanceRepository extends JpaRepository<AlertInstance, Long> {

    List<AlertInstance> findByResolvedFalseOrderByAlertTimeDesc();

    List<AlertInstance> findByResolvedFalseAndSeverityOrderByAlertTimeDesc(AlertSeverity severity);

    Page<AlertInstance> findByRuleIdOrderByAlertTimeDesc(Long ruleId, Pageable pageable);

    @Query("SELECT a FROM AlertInstance a WHERE a.resolved = false AND a.alertTime < :before")
    List<AlertInstance> findUnresolvedAlertsBefore(@Param("before") LocalDateTime before);

    @Query("SELECT COUNT(a) FROM AlertInstance a WHERE a.ruleId = :ruleId AND a.resolved = false AND a.alertTime BETWEEN :start AND :end")
    long countUnresolvedByRuleInTimeRange(
            @Param("ruleId") Long ruleId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    List<AlertInstance> findByTransactionId(String transactionId);

    void deleteByAlertTimeBefore(LocalDateTime cutoffDate);
}
