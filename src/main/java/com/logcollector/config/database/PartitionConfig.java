package com.logcollector.config.database;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * MySQL table partitioning support for log_entries table.
 *
 * Automatically creates future partitions to prevent "no partition found" errors.
 */
@Configuration
@Slf4j
public class PartitionConfig {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Creates partitions for the next 6 months if they don't exist.
     * Runs monthly to ensure future partitions are always available.
     */
    @Scheduled(cron = "0 0 1 1 * ?") // First day of each month at 1 AM
    public void createFuturePartitions() {
        log.info("Creating future partitions for log_entries table");

        LocalDate now = LocalDate.now();
        for (int i = 0; i < 6; i++) {
            LocalDate partitionDate = now.plusMonths(i);
            createPartitionIfNotExists(partitionDate);
        }

        log.info("Partition creation completed");
    }

    private void createPartitionIfNotExists(LocalDate date) {
        String partitionName = "p" + date.format(DateTimeFormatter.ofPattern("yyyyMM"));
        int partitionValue = date.getYear() * 100 + date.getMonthValue() + 1;

        try {
            String sql = String.format(
                    "ALTER TABLE log_entries REORGANIZE PARTITION p_future INTO " +
                            "(PARTITION %s VALUES LESS THAN (%d), PARTITION p_future VALUES LESS THAN MAXVALUE)",
                    partitionName, partitionValue
            );

            jdbcTemplate.execute(sql);
            log.info("Created partition: {} for value < {}", partitionName, partitionValue);
        } catch (Exception e) {
            // Partition might already exist, log and continue
            log.debug("Partition {} may already exist or error occurred: {}", partitionName, e.getMessage());
        }
    }
}
