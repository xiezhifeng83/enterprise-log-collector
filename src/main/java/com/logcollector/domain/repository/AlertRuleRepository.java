package com.logcollector.domain.repository;

import com.logcollector.domain.model.AlertRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for AlertRule entity.
 */
@Repository
public interface AlertRuleRepository extends JpaRepository<AlertRule, Long> {

    Optional<AlertRule> findByName(String name);

    List<AlertRule> findByEnabled(Boolean enabled);

    List<AlertRule> findByEnabledAndAlertType(Boolean enabled, String alertType);

    @Query("SELECT a FROM AlertRule a WHERE a.enabled = true ORDER BY a.severity DESC, a.name ASC")
    List<AlertRule> findAllEnabledOrderedBySeverity();

    boolean existsByName(String name);

    @Query("SELECT a FROM AlertRule a WHERE a.createdBy = :userId")
    List<AlertRule> findByCreatedBy(@Param("userId") Long userId);
}
