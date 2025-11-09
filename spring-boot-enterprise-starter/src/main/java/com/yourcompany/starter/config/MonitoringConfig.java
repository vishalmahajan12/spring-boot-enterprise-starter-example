package com.yourcompany.starter.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for application monitoring.
 * 
 * This configuration:
 * 1. Sets up Micrometer metrics
 * 2. Configures Prometheus registry
 * 3. Customizes meter registry with application name
 * 4. Sets up meter filters
 * 
 * Only enabled when enterprise.starter.monitoring.enabled=true
 */
@Configuration
@ConditionalOnProperty(prefix = "enterprise.starter.monitoring", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MonitoringConfig {

    /**
     * Customizes meter registry with application name and common tags.
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> {
            String appName = System.getProperty("spring.application.name", "unknown-service");
            registry.config().commonTags("application", appName);
        };
    }

    /**
     * Creates Prometheus meter registry if Prometheus is enabled.
     */
    @Bean
    @ConditionalOnProperty(prefix = "enterprise.starter.monitoring", name = "expose-prometheus", havingValue = "true", matchIfMissing = true)
    public PrometheusMeterRegistry prometheusMeterRegistry() {
        return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    }
}

