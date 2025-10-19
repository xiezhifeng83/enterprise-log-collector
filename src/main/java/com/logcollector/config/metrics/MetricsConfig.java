package com.logcollector.config.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * Micrometer metrics configuration for Prometheus.
 *
 * Configures common tags and custom metrics for monitoring.
 */
@Configuration
public class MetricsConfig {

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config().commonTags(
                Arrays.asList(
                        Tag.of("application", applicationName),
                        Tag.of("environment", activeProfile)
                )
        );
    }

    /**
     * Custom metrics will be registered by services:
     *
     * Business metrics:
     * - logs.collected.total
     * - logs.processed.total
     * - transactions.analyzed.total
     * - alerts.triggered.total
     *
     * System metrics (auto-registered by Spring):
     * - jvm.memory.used
     * - hikari.connections.active
     * - kafka.consumer.lag
     *
     * Performance metrics:
     * - log.processing.latency (p50/p95/p99)
     * - transaction.analysis.duration
     */
}
