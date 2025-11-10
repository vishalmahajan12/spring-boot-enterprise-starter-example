package com.yourcompany.sample.controller;

import com.yourcompany.sample.service.CachedDataService;
import com.yourcompany.sample.service.ExternalServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Demo endpoints for testing various Enterprise Starter features.
 */
@RestController
@RequestMapping("/api/demo")
public class DemoController {

    @Autowired
    private ExternalServiceClient externalServiceClient;
    
    @Autowired
    private CachedDataService cachedDataService;

    @GetMapping("/logging")
    public ResponseEntity<Map<String, Object>> testLogging(
            @RequestParam(required = false) String password,
            @RequestParam(required = false) String token) {
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Check logs for request/response logging");
        response.put("note", "Sensitive fields like password and token should be masked");
        response.put("correlationId", "Check response headers for X-Correlation-ID");
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logging")
    public ResponseEntity<Map<String, Object>> testLoggingPost(@RequestBody Map<String, Object> body) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "POST request logged");
        response.put("received", body);
        response.put("note", "Check logs for request body logging");
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/slow-operation")
    public ResponseEntity<Map<String, Object>> testSlowOperation() throws InterruptedException {
        String result = externalServiceClient.performSlowOperation();
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", result);
        response.put("note", "Check logs for slow query detection warning");
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/external-api")
    public ResponseEntity<Map<String, Object>> testExternalApi(
            @RequestParam(defaultValue = "https://httpbin.org/json") String url) {
        
        Map<String, Object> result = externalServiceClient.callExternalApi(url);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "External API called");
        response.put("result", result);
        response.put("note", "Check logs for outgoing request logging");
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/error")
    public ResponseEntity<Map<String, Object>> testError(@RequestParam(defaultValue = "false") boolean shouldFail) {
        if (shouldFail) {
            throw new RuntimeException("Test error for error tracking");
        }
        
        return ResponseEntity.ok(Map.of("message", "No error occurred"));
    }

    @GetMapping("/rate-limit")
    public ResponseEntity<Map<String, Object>> testRateLimit() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Rate limit test");
        response.put("note", "Make multiple requests quickly to test rate limiting");
        response.put("limit", "10 requests per minute");
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/cache/config")
    public ResponseEntity<Map<String, Object>> testCacheConfig(@RequestParam String key) {
        long startTime = System.currentTimeMillis();
        String value = cachedDataService.getConfig(key);
        long duration = System.currentTimeMillis() - startTime;
        
        Map<String, Object> response = new HashMap<>();
        response.put("key", key);
        response.put("value", value);
        response.put("durationMs", duration);
        response.put("note", "First call: slow (cache miss). Subsequent calls: fast (cache hit)");
        response.put("tip", "Try calling this endpoint multiple times with the same key");
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/cache/user")
    public ResponseEntity<Map<String, Object>> testCacheUser(@RequestParam Long id) {
        long startTime = System.currentTimeMillis();
        Map<String, Object> user = cachedDataService.getUser(id);
        long duration = System.currentTimeMillis() - startTime;
        
        Map<String, Object> response = new HashMap<>();
        response.put("user", user);
        response.put("durationMs", duration);
        response.put("note", "First call: slow (cache miss). Subsequent calls: fast (cache hit)");
        response.put("tip", "Try calling this endpoint multiple times with the same id");
        
        return ResponseEntity.ok(response);
    }

    @PutMapping("/cache/config")
    public ResponseEntity<Map<String, Object>> updateCacheConfig(
            @RequestParam String key,
            @RequestParam String value) {
        cachedDataService.updateConfig(key, value);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Config updated and cache refreshed");
        response.put("key", key);
        response.put("value", value);
        response.put("note", "Cache is automatically updated with new value");
        
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/cache/config")
    public ResponseEntity<Map<String, Object>> evictCacheConfig(@RequestParam String key) {
        cachedDataService.evictConfig(key);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Config evicted from cache");
        response.put("key", key);
        response.put("note", "Cache entry removed. Next call will fetch from source");
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/cache/config/clear")
    public ResponseEntity<Map<String, Object>> clearConfigCache() {
        cachedDataService.clearConfigCache();
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "All config cache entries cleared");
        response.put("note", "All cache entries removed. Next calls will fetch from source");
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/cache/api-response")
    public ResponseEntity<Map<String, Object>> testCacheApiResponse(@RequestParam String endpoint) {
        long startTime = System.currentTimeMillis();
        String response = cachedDataService.getCachedApiResponse(endpoint);
        long duration = System.currentTimeMillis() - startTime;
        
        Map<String, Object> result = new HashMap<>();
        result.put("endpoint", endpoint);
        result.put("response", response);
        result.put("durationMs", duration);
        result.put("note", "First call: slow (simulated API call). Subsequent calls: fast (cache hit)");
        result.put("tip", "Try calling this endpoint multiple times with the same endpoint");
        
        return ResponseEntity.ok(result);
    }

    @GetMapping("/cache/uncached")
    public ResponseEntity<Map<String, Object>> testUncached(@RequestParam String key) {
        long startTime = System.currentTimeMillis();
        String value = cachedDataService.getUncachedData(key);
        long duration = System.currentTimeMillis() - startTime;
        
        Map<String, Object> response = new HashMap<>();
        response.put("key", key);
        response.put("value", value);
        response.put("durationMs", duration);
        response.put("note", "This endpoint does NOT use caching - always slow");
        response.put("comparison", "Compare with /api/demo/cache/config to see caching benefits");
        
        return ResponseEntity.ok(response);
    }
}

