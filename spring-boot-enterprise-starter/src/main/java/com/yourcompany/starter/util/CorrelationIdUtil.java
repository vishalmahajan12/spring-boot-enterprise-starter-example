package com.yourcompany.starter.util;

import java.util.UUID;

/**
 * Utility class for managing correlation IDs using ThreadLocal.
 * 
 * Correlation IDs are used to trace requests across multiple services.
 * This class:
 * 1. Generates unique correlation IDs
 * 2. Stores them in ThreadLocal (one per request thread)
 * 3. Provides access throughout the request lifecycle
 * 4. Cleans up after request completion
 * 
 * Usage:
 * - Set correlation ID at the start of request
 * - Get it anywhere in the request thread
 * - Clear it at the end of request
 */
public class CorrelationIdUtil {
    private static final ThreadLocal<String> correlationIdHolder = new ThreadLocal<>();

    /**
     * Generates a new UUID-based correlation ID.
     */
    public static String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Sets the correlation ID for the current thread.
     */
    public static void setCorrelationId(String correlationId) {
        correlationIdHolder.set(correlationId);
    }

    /**
     * Gets the correlation ID for the current thread.
     * If not set, generates a new one automatically.
     */
    public static String getCorrelationId() {
        String id = correlationIdHolder.get();
        if (id == null) {
            id = generateCorrelationId();
            setCorrelationId(id);
        }
        return id;
    }

    /**
     * Clears the correlation ID from ThreadLocal.
     * Should be called at the end of request processing.
     */
    public static void clear() {
        correlationIdHolder.remove();
    }
}

