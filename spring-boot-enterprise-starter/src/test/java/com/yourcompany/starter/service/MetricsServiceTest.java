package com.yourcompany.starter.service;

import com.yourcompany.starter.config.EnterpriseStarterProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MetricsServiceTest {

    private MetricsService metricsService;
    private MeterRegistry meterRegistry;
    private EnterpriseStarterProperties properties;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        properties = new EnterpriseStarterProperties();
        properties.getMonitoring().setEnabled(true);
        properties.getMonitoring().setEnableMetrics(true);
        properties.getMonitoring().setEnableErrorTracking(true);
        
        metricsService = new MetricsService(meterRegistry, properties);
    }

    @Test
    void testRecordHttpRequest() {
        metricsService.recordHttpRequest("GET", "/api/users", 200, 150);
        
        // Verify timer was recorded
        assertNotNull(meterRegistry.find("http.request.duration")
                .tag("method", "GET")
                .tag("path", "/api/users")
                .tag("status", "200")
                .timer());
        
        // Verify counter was incremented
        assertNotNull(meterRegistry.find("http.request.count")
                .tag("method", "GET")
                .tag("path", "/api/users")
                .tag("status", "200")
                .counter());
    }

    @Test
    void testRecordError() {
        metricsService.recordError("ValidationError", "Invalid input parameter");
        
        assertNotNull(meterRegistry.find("application.errors")
                .tag("type", "ValidationError")
                .counter());
    }

    @Test
    void testRecordBusinessMetric() {
        metricsService.recordBusinessMetric("orders.processed", 1.0, "region", "us-east");
        
        assertNotNull(meterRegistry.find("business.metric.orders.processed")
                .tag("region", "us-east")
                .counter());
    }

    @Test
    void testRecordExecutionTime() {
        metricsService.recordExecutionTime("database.query", 250);
        
        assertNotNull(meterRegistry.find("operation.execution.time")
                .tag("operation", "database.query")
                .timer());
    }

    @Test
    void testMetricsDisabled() {
        properties.getMonitoring().setEnableMetrics(false);
        MetricsService disabledService = new MetricsService(meterRegistry, properties);
        
        // Should not throw exception when metrics disabled
        assertDoesNotThrow(() -> {
            disabledService.recordHttpRequest("GET", "/api", 200, 100);
            disabledService.recordError("Error", "Message");
            disabledService.recordBusinessMetric("metric", 1.0);
            disabledService.recordExecutionTime("op", 100);
        });
    }

    @Test
    void testErrorTrackingDisabled() {
        properties.getMonitoring().setEnableErrorTracking(false);
        MetricsService service = new MetricsService(meterRegistry, properties);
        
        // Should not throw exception when error tracking disabled
        assertDoesNotThrow(() -> {
            service.recordError("Error", "Message");
        });
    }
}

