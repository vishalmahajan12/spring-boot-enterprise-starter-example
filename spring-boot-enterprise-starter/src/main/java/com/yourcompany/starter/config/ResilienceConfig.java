package com.yourcompany.starter.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration for Resilience4j Circuit Breaker and Retry mechanisms.
 * 
 * Provides fault tolerance for external service calls:
 * 1. Circuit Breaker - prevents cascading failures
 * 2. Retry - automatically retries failed requests
 * 
 * Configuration can be customized via application properties.
 */
@Configuration
@ConditionalOnProperty(prefix = "enterprise.starter.resilience", name = "enabled", havingValue = "true", matchIfMissing = false)
public class ResilienceConfig {

    /**
     * Creates Circuit Breaker registry with default configuration.
     * Default settings:
     * - Failure rate threshold: 50%
     * - Wait duration in open state: 60 seconds
     * - Sliding window size: 10 requests
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50) // 50% failure rate triggers circuit breaker
                .waitDurationInOpenState(Duration.ofSeconds(60)) // Wait 60s before trying again
                .slidingWindowSize(10) // Last 10 requests are considered
                .minimumNumberOfCalls(5) // Need at least 5 calls before evaluating
                .permittedNumberOfCallsInHalfOpenState(3) // Allow 3 calls in half-open state
                .build();

        return CircuitBreakerRegistry.of(config);
    }

    /**
     * Creates Retry registry with default configuration.
     * Default settings:
     * - Max attempts: 3
     * - Wait duration: 1 second
     * - Exponential backoff: enabled
     */
    @Bean
    public RetryRegistry retryRegistry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3) // Retry up to 3 times
                .waitDuration(Duration.ofSeconds(1)) // Wait 1 second between retries
                .retryExceptions(Exception.class) // Retry on any exception
                .build();

        return RetryRegistry.of(config);
    }

    /**
     * Creates a default Circuit Breaker instance.
     * Applications can create named instances for specific services.
     */
    @Bean
    public CircuitBreaker defaultCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("default");
    }

    /**
     * Creates a default Retry instance.
     * Applications can create named instances for specific operations.
     */
    @Bean
    public Retry defaultRetry(RetryRegistry registry) {
        return registry.retry("default");
    }
}

