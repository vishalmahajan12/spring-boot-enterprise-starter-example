package com.yourcompany.sample.service;

import com.yourcompany.starter.annotation.MonitorPerformance;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Service demonstrating external API calls with resilience patterns.
 * Shows logging, circuit breaker, and retry mechanisms.
 */
@Service
public class ExternalServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(ExternalServiceClient.class);

    @Autowired(required = false)
    private RestTemplate restTemplate;

    @Autowired(required = false)
    private CircuitBreaker circuitBreaker;

    @Autowired(required = false)
    private Retry retry;

    /**
     * Example: Call external API with circuit breaker and retry.
     * Demonstrates resilience patterns.
     */
    @MonitorPerformance
    public Map<String, Object> callExternalApi(String endpoint) {
        if (restTemplate == null) {
            logger.warn("RestTemplate not available");
            return Map.of("error", "RestTemplate not configured");
        }

        try {
            // Use circuit breaker if available
            if (circuitBreaker != null) {
                return circuitBreaker.executeSupplier(() -> {
                    logger.info("Calling external API: {}", endpoint);
                    ResponseEntity<Map> response = restTemplate.getForEntity(
                        endpoint, Map.class);
                    return response.getBody() != null ? response.getBody() : Map.of();
                });
            } else {
                // Fallback without circuit breaker
                logger.info("Calling external API (no circuit breaker): {}", endpoint);
                ResponseEntity<Map> response = restTemplate.getForEntity(endpoint, Map.class);
                return response.getBody() != null ? response.getBody() : Map.of();
            }
        } catch (Exception e) {
            logger.error("Error calling external API", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "External API call failed");
            errorResponse.put("message", e.getMessage());
            return errorResponse;
        }
    }

    /**
     * Example: Simulate slow operation to test slow query detection.
     */
    @MonitorPerformance
    public String performSlowOperation() throws InterruptedException {
        logger.info("Performing slow operation...");
        Thread.sleep(1500); // Simulate slow operation (> 1000ms threshold)
        return "Slow operation completed";
    }

    /**
     * Example: Simulate operation that might fail.
     */
    @MonitorPerformance
    public String performRiskyOperation(boolean shouldFail) {
        if (shouldFail) {
            throw new RuntimeException("Simulated failure");
        }
        return "Operation successful";
    }
}

