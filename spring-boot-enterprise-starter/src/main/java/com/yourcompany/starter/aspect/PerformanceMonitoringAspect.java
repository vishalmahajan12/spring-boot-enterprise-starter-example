package com.yourcompany.starter.aspect;

import com.yourcompany.starter.config.EnterpriseStarterProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Aspect for monitoring method performance.
 * 
 * This aspect:
 * 1. Measures execution time of methods
 * 2. Tracks slow queries/operations
 * 3. Records metrics to Micrometer
 * 4. Logs slow operations for investigation
 * 
 * Only enabled when enterprise.starter.monitoring.enabled=true
 */
@Aspect
@Component
@ConditionalOnProperty(prefix = "enterprise.starter.monitoring", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PerformanceMonitoringAspect {
    private static final Logger logger = LoggerFactory.getLogger(PerformanceMonitoringAspect.class);
    private final EnterpriseStarterProperties properties;
    private final MeterRegistry meterRegistry;

    public PerformanceMonitoringAspect(EnterpriseStarterProperties properties, MeterRegistry meterRegistry) {
        this.properties = properties;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Monitors execution time of methods annotated with @MonitorPerformance.
     */
    @Around("@annotation(com.yourcompany.starter.annotation.MonitorPerformance)")
    public Object monitorPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!properties.getMonitoring().isEnableMetrics()) {
            return joinPoint.proceed();
        }

        String methodName = joinPoint.getSignature().toShortString();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        Timer.Sample sample = Timer.start(meterRegistry);
        long startTime = System.currentTimeMillis();
        
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            
            // Record metric
            sample.stop(Timer.builder("method.execution.time")
                    .tag("class", className)
                    .tag("method", methodName)
                    .register(meterRegistry));
            
            // Check for slow queries
            if (properties.getMonitoring().isEnableSlowQueryDetection() &&
                duration > properties.getMonitoring().getSlowQueryThresholdMs()) {
                logger.warn("Slow operation detected: {} took {}ms (threshold: {}ms)", 
                        methodName, duration, properties.getMonitoring().getSlowQueryThresholdMs());
            }
            
            return result;
        } catch (Throwable e) {
            // Record error metric
            meterRegistry.counter("method.execution.errors",
                    "class", className,
                    "method", methodName,
                    "exception", e.getClass().getSimpleName()).increment();
            throw e;
        }
    }
}

