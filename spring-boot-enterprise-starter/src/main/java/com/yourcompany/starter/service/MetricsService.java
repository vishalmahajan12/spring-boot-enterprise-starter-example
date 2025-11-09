package com.yourcompany.starter.service;

import com.yourcompany.starter.config.EnterpriseStarterProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Service for recording custom metrics.
 * 
 * Provides convenient methods to record:
 * 1. Request counts
 * 2. Error counts
 * 3. Response times
 * 4. Custom business metrics
 * 
 * Only enabled when enterprise.starter.monitoring.enabled=true
 */
@Service
@ConditionalOnProperty(prefix = "enterprise.starter.monitoring", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MetricsService {
    private final MeterRegistry meterRegistry;
    private final EnterpriseStarterProperties properties;

    public MetricsService(MeterRegistry meterRegistry, EnterpriseStarterProperties properties) {
        this.meterRegistry = meterRegistry;
        this.properties = properties;
    }

    /**
     * Records an HTTP request metric.
     */
    public void recordHttpRequest(String method, String path, int statusCode, long durationMs) {
        if (!properties.getMonitoring().isEnableMetrics()) {
            return;
        }

        Timer.builder("http.request.duration")
                .tag("method", method)
                .tag("path", path)
                .tag("status", String.valueOf(statusCode))
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);

        Counter.builder("http.request.count")
                .tag("method", method)
                .tag("path", path)
                .tag("status", String.valueOf(statusCode))
                .register(meterRegistry)
                .increment();
    }

    /**
     * Records an error metric.
     */
    public void recordError(String errorType, String errorMessage) {
        if (!properties.getMonitoring().isEnableErrorTracking()) {
            return;
        }

        Counter.builder("application.errors")
                .tag("type", errorType)
                .tag("message", errorMessage != null ? errorMessage.substring(0, Math.min(50, errorMessage.length())) : "unknown")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Records a custom business metric.
     */
    public void recordBusinessMetric(String metricName, double value, String... tags) {
        if (!properties.getMonitoring().isEnableMetrics()) {
            return;
        }

        // Simple counter - can be extended for gauges, histograms, etc.
        Counter.builder("business.metric." + metricName)
                .tags(tags)
                .register(meterRegistry)
                .increment((long) value);
    }

    /**
     * Records execution time for an operation.
     */
    public void recordExecutionTime(String operationName, long durationMs) {
        if (!properties.getMonitoring().isEnableMetrics()) {
            return;
        }

        Timer.builder("operation.execution.time")
                .tag("operation", operationName)
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }
}

