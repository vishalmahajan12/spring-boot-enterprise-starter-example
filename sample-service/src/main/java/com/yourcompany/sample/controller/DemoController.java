package com.yourcompany.sample.controller;

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
}

