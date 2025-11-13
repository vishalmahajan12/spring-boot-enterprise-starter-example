package com.yourcompany.sample.controller;

import com.yourcompany.sample.service.CachedDataService;
import com.yourcompany.sample.service.ExternalServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
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
    
    @Autowired
    private ApplicationContext applicationContext;

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

    @GetMapping("/cache/diagnostics")
    public ResponseEntity<Map<String, Object>> cacheDiagnostics() {
        Map<String, Object> diagnostics = new HashMap<>();
        
        try {
            CacheManager cacheManager = applicationContext.getBean(CacheManager.class);
            
            if (cacheManager != null) {
                diagnostics.put("cacheManagerType", cacheManager.getClass().getName());
                diagnostics.put("cacheNames", cacheManager.getCacheNames());
                
                // Check if it's our MultiLevelCacheManager
                if (cacheManager instanceof com.yourcompany.starter.cache.MultiLevelCacheManager) {
                    diagnostics.put("isMultiLevel", true);
                    diagnostics.put("note", "Multi-level cache manager is active (L1 + L2)");
                } else {
                    diagnostics.put("isMultiLevel", false);
                    diagnostics.put("note", "Single-level cache manager (L1 only)");
                }
                
                // Check if RedisCacheConfig is loaded
                try {
                    Object redisCacheConfig = applicationContext.getBean("redisCacheConfig");
                    diagnostics.put("redisCacheConfigExists", true);
                    diagnostics.put("redisCacheConfigType", redisCacheConfig.getClass().getName());
                } catch (Exception e) {
                    diagnostics.put("redisCacheConfigExists", false);
                    diagnostics.put("redisCacheConfigError", "Bean not found: " + e.getMessage());
                }
                
                // Check for RedisConnectionFactory bean
                try {
                    Object redisConnectionFactory = applicationContext.getBean("redisConnectionFactory");
                    diagnostics.put("redisConnectionFactoryExists", true);
                    diagnostics.put("redisConnectionFactoryType", redisConnectionFactory.getClass().getName());
                } catch (Exception e) {
                    diagnostics.put("redisConnectionFactoryExists", false);
                    diagnostics.put("redisConnectionFactoryError", "Bean not found: " + e.getMessage());
                }
                
                // Check for Redis cache manager bean
                try {
                    CacheManager redisCacheManager = applicationContext.getBean("redisCacheManager", CacheManager.class);
                    diagnostics.put("redisCacheManagerExists", true);
                    diagnostics.put("redisCacheManagerType", redisCacheManager.getClass().getName());
                } catch (Exception e) {
                    diagnostics.put("redisCacheManagerExists", false);
                    diagnostics.put("redisCacheManagerError", "Bean not found: " + e.getMessage());
                }
                
                // Check for Caffeine cache manager bean
                try {
                    CacheManager caffeineCacheManager = applicationContext.getBean("caffeineCacheManager", CacheManager.class);
                    diagnostics.put("caffeineCacheManagerExists", true);
                    diagnostics.put("caffeineCacheManagerType", caffeineCacheManager.getClass().getName());
                } catch (Exception e) {
                    diagnostics.put("caffeineCacheManagerExists", false);
                }
            } else {
                diagnostics.put("error", "No cache manager found");
            }
        } catch (Exception e) {
            diagnostics.put("error", e.getMessage());
            diagnostics.put("exception", e.getClass().getName());
            diagnostics.put("stackTrace", getStackTrace(e));
        }
        
        return ResponseEntity.ok(diagnostics);
    }
    
    private String getStackTrace(Exception e) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}

