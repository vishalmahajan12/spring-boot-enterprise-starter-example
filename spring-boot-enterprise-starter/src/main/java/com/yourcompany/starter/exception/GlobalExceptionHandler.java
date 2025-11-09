package com.yourcompany.starter.exception;

import com.yourcompany.starter.config.EnterpriseStarterProperties;
import com.yourcompany.starter.service.MetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the application.
 * 
 * This handler:
 * 1. Catches all unhandled exceptions
 * 2. Logs error details
 * 3. Records error metrics
 * 4. Returns standardized error responses
 * 5. Masks sensitive error information in production
 * 
 * Only enabled when enterprise.starter.monitoring.enable-error-tracking=true
 */
@RestControllerAdvice
@ConditionalOnProperty(prefix = "enterprise.starter.monitoring", name = "enable-error-tracking", havingValue = "true", matchIfMissing = true)
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final MetricsService metricsService;
    private final EnterpriseStarterProperties properties;

    public GlobalExceptionHandler(MetricsService metricsService, EnterpriseStarterProperties properties) {
        this.metricsService = metricsService;
        this.properties = properties;
    }

    /**
     * Handles all exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception ex) {
        logger.error("Unhandled exception occurred", ex);

        // Record error metric
        if (properties.getMonitoring().isEnableErrorTracking()) {
            metricsService.recordError(ex.getClass().getSimpleName(), ex.getMessage());
        }

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        errorResponse.put("error", "Internal Server Error");
        
        // Only include message in non-production environments
        if (properties.getMonitoring().isEnableErrorTracking()) {
            errorResponse.put("message", ex.getMessage());
            if (logger.isDebugEnabled()) {
                errorResponse.put("stackTrace", getStackTrace(ex));
            }
        } else {
            errorResponse.put("message", "An error occurred. Please contact support.");
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Handles IllegalArgumentException.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        logger.warn("Illegal argument: {}", ex.getMessage());

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
        errorResponse.put("error", "Bad Request");
        errorResponse.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Gets stack trace as string.
     */
    private String getStackTrace(Exception ex) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        ex.printStackTrace(pw);
        return sw.toString();
    }
}

