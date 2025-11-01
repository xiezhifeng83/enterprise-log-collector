package com.logcollector.domain.repository;

import com.logcollector.domain.model.ArchiveJob;
import com.logcollector.domain.model.ArchiveJob.ArchiveStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for ArchiveJob entity.
 */
@Repository
public interface ArchiveJobRepository extends JpaRepository<ArchiveJob, Long> {

    List<ArchiveJob> findByStatusOrderByExecutionTimeDesc(ArchiveStatus status);

    Page<ArchiveJob> findByStatusOrderByExecutionTimeDesc(ArchiveStatus status, Pageable pageable);

    @Query("SELECT a FROM ArchiveJob a WHERE a.startDate <= :date AND a.endDate >= :date AND a.status = 'COMPLETED'")
    List<ArchiveJob> findCompletedJobsCoveringDate(@Param("date") LocalDate date);

    @Query("SELECT a FROM ArchiveJob a WHERE a.status = 'RUNNING' AND a.executionTime < :before")
    List<ArchiveJob> findStaleRunningJobs(@Param("before") LocalDateTime before);

    Page<ArchiveJob> findAllByOrderByExecutionTimeDesc(Pageable pageable);
}
