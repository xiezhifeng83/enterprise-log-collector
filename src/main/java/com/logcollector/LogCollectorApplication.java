package com.logcollector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for Enterprise Log Collection and Analysis System.
 *
 * This application provides:
 * - Automated log collection from multiple Linux servers via SSH
 * - Transaction flow analysis across distributed systems
 * - Intelligent alerting on anomalies
 * - Historical search and compliance reporting
 * - Data lifecycle management with multi-tier storage
 *
 * @author Enterprise Log Collector Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableScheduling
public class LogCollectorApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogCollectorApplication.class, args);
    }
}
