package com.yourcompany.starter.config;

import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Configuration for application monitoring.
 * 
 * This configuration:
 * 1. Sets up Micrometer metrics
 * 2. Customizes meter registry with application name
 * 3. Configures common tags for metrics
 * 
 * Note: Prometheus support is auto-configured by Spring Boot Actuator when
 * micrometer-registry-prometheus is on the classpath. To expose the Prometheus
 * endpoint, configure management.endpoints.web.exposure.include in application.yml.
 * 
 * Only enabled when enterprise.starter.monitoring.enabled=true
 */
@Configuration
@ConditionalOnProperty(prefix = "enterprise.starter.monitoring", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MonitoringConfig {

    /**
     * Customizes meter registry with application name and common tags.
     * This applies to all meter registries including Prometheus (when available).
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> {
            String appName = System.getProperty("spring.application.name", "unknown-service");
            registry.config().commonTags("application", appName);
        };
    }

    // Note: PrometheusMeterRegistry is auto-configured by Spring Boot Actuator
    // when micrometer-registry-prometheus is on the classpath.
    // Creating a custom bean here would conflict with Spring Boot's auto-configuration.
    // To expose the Prometheus endpoint, add 'prometheus' to
    // management.endpoints.web.exposure.include in application.yml
}

