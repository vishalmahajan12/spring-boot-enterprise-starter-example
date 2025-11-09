package com.yourcompany.starter.service;

import com.yourcompany.starter.config.EnterpriseStarterProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for rate limiting functionality.
 * 
 * Implements simple in-memory rate limiting based on:
 * - IP address
 * - User ID (if authenticated)
 * - Endpoint/operation
 * 
 * Uses sliding window algorithm with per-minute limits.
 * 
 * Note: For production, consider using Redis-based distributed rate limiting.
 */
@Service
@ConditionalOnProperty(prefix = "enterprise.starter.authentication", name = "enable-rate-limiting", havingValue = "true")
public class RateLimitingService {
    private static final Logger logger = LoggerFactory.getLogger(RateLimitingService.class);
    private final EnterpriseStarterProperties properties;
    
    // In-memory storage for rate limiting (key -> request count for current minute)
    private final Map<String, RateLimitWindow> rateLimitMap = new ConcurrentHashMap<>();
    
    // Cleanup thread to remove expired entries
    private static final long CLEANUP_INTERVAL_MS = 60000; // 1 minute
    
    public RateLimitingService(EnterpriseStarterProperties properties) {
        this.properties = properties;
        startCleanupThread();
    }

    /**
     * Checks if request should be allowed based on rate limiting rules.
     * @param identifier Unique identifier (IP address, user ID, etc.)
     * @return true if request is allowed, false if rate limit exceeded
     */
    public boolean isAllowed(String identifier) {
        if (!properties.getAuthentication().isEnableRateLimiting()) {
            return true;
        }

        int limitPerMinute = properties.getAuthentication().getRateLimitPerMinute();
        if (limitPerMinute <= 0) {
            return true; // No limit
        }

        long currentMinute = System.currentTimeMillis() / 60000; // Current minute timestamp
        
        RateLimitWindow window = rateLimitMap.computeIfAbsent(identifier, 
                k -> new RateLimitWindow(currentMinute));
        
        // If window is for a different minute, reset it
        if (window.minute != currentMinute) {
            window.minute = currentMinute;
            window.count.set(0);
        }
        
        // Increment and check limit
        int currentCount = window.count.incrementAndGet();
        
        if (currentCount > limitPerMinute) {
            logger.warn("Rate limit exceeded for identifier: {}, count: {}, limit: {}", 
                    identifier, currentCount, limitPerMinute);
            return false;
        }
        
        return true;
    }

    /**
     * Gets current request count for an identifier.
     */
    public int getCurrentCount(String identifier) {
        RateLimitWindow window = rateLimitMap.get(identifier);
        if (window == null) {
            return 0;
        }
        long currentMinute = System.currentTimeMillis() / 60000;
        if (window.minute != currentMinute) {
            return 0;
        }
        return window.count.get();
    }

    /**
     * Starts background thread to clean up expired rate limit entries.
     */
    private void startCleanupThread() {
        Thread cleanupThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(CLEANUP_INTERVAL_MS);
                    cleanupExpiredEntries();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        cleanupThread.setDaemon(true);
        cleanupThread.setName("rate-limit-cleanup");
        cleanupThread.start();
    }

    /**
     * Removes expired rate limit entries (older than 2 minutes).
     */
    private void cleanupExpiredEntries() {
        long currentMinute = System.currentTimeMillis() / 60000;
        rateLimitMap.entrySet().removeIf(entry -> 
                currentMinute - entry.getValue().minute > 2);
    }

    /**
     * Clears all rate limit entries.
     * Useful for testing or resetting rate limits.
     */
    public void clearAll() {
        rateLimitMap.clear();
    }

    /**
     * Inner class to hold rate limit window data.
     */
    private static class RateLimitWindow {
        volatile long minute;
        final AtomicInteger count = new AtomicInteger(0);

        RateLimitWindow(long minute) {
            this.minute = minute;
        }
    }
}

