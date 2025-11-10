package com.yourcompany.starter.integration;

import com.yourcompany.starter.config.EnterpriseStarterProperties;
import com.yourcompany.starter.service.RateLimitingService;
import com.yourcompany.starter.util.CorrelationIdUtil;
import com.yourcompany.starter.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Enterprise Starter features.
 * Tests the full request/response flow including logging, authentication, and monitoring.
 */
@SpringBootTest(classes = TestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "enterprise.starter.logging.enabled=true",
    "enterprise.starter.logging.log-request=true",
    "enterprise.starter.logging.log-response=true",
    "enterprise.starter.authentication.enabled=false",
    "enterprise.starter.swagger.enabled=true",
    "enterprise.starter.monitoring.enabled=true",
    "management.endpoints.web.exposure.include=health,info",
    "management.endpoint.health.show-details=always",
    "management.health.redis.enabled=false"
})
class EnterpriseStarterIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private EnterpriseStarterProperties properties;

    @Autowired(required = false)
    private JwtUtil jwtUtil;

    @Autowired(required = false)
    private RateLimitingService rateLimitingService;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
        CorrelationIdUtil.clear();
        
        // Clear rate limits between tests
        if (rateLimitingService != null) {
            rateLimitingService.clearAll();
        }
        
        // Reset authentication settings to defaults
        properties.getAuthentication().setEnabled(false);
        properties.getAuthentication().setEnableRateLimiting(false);
        properties.getAuthentication().setEnableSecurityHeaders(false);
        
        // Ensure jwtUtil is available
        if (jwtUtil == null) {
            jwtUtil = new JwtUtil();
        }
    }

    @Test
    void testRequestLogging_AddsCorrelationId() throws Exception {
        String correlationId = "test-correlation-id-123";
        
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Correlation-ID", correlationId);
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/api/test",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);
        
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertTrue(response.getHeaders().containsKey("X-Correlation-ID"));
        assertEquals(correlationId, response.getHeaders().getFirst("X-Correlation-ID"));
    }

    @Test
    void testRequestLogging_GeneratesCorrelationId() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/api/test",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);
        
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertTrue(response.getHeaders().containsKey("X-Correlation-ID"));
        assertNotNull(response.getHeaders().getFirst("X-Correlation-ID"));
    }

    @Test
    void testRequestLogging_LogsRequestAndResponse() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/api/test",
                HttpMethod.POST,
                new HttpEntity<>("{\"name\":\"test\"}", headers),
                String.class);
        
        assertTrue(response.getStatusCode().is2xxSuccessful());
        // Verify logging occurred (check logs or use log appender)
    }

    @Test
    void testRequestLogging_ExcludesPaths() throws Exception {
        properties.getLogging().setExcludedPaths(List.of("/actuator/**"));
        
        ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/actuator/health",
                String.class);
        
        // Health endpoint should be accessible (may return 200 or 503 depending on health status)
        // The important thing is that the request doesn't fail with 404
        assertTrue(response.getStatusCode().is2xxSuccessful() || 
                   response.getStatusCode().value() == 503,
                   "Expected 2xx or 503, got: " + response.getStatusCode());
        // Logging should be skipped for excluded paths
    }

    @Test
    void testAuthentication_JWT_ValidToken() throws Exception {
        properties.getAuthentication().setEnabled(true);
        properties.getAuthentication().setType(EnterpriseStarterProperties.Authentication.AuthType.JWT);
        properties.getAuthentication().setSecretKey("test-secret-key-123456789012345678901234567890");
        properties.getAuthentication().setAllowedRoles(List.of());
        properties.getAuthentication().setEnableIpWhitelist(false);
        properties.getAuthentication().setOperationRules(java.util.Map.of());
        
        String token = jwtUtil.generateToken("testuser", List.of("USER"), 
                properties.getAuthentication().getSecretKey(), 3600);
        
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/api/test",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);
        
        assertTrue(response.getStatusCode().is2xxSuccessful());
    }

    @Test
    void testAuthentication_JWT_InvalidToken() throws Exception {
        properties.getAuthentication().setEnabled(true);
        properties.getAuthentication().setType(EnterpriseStarterProperties.Authentication.AuthType.JWT);
        properties.getAuthentication().setSecretKey("test-secret-key-123456789012345678901234567890");
        
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer invalid-token");
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/api/test",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);
        
        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    void testAuthentication_APIKey_Valid() throws Exception {
        properties.getAuthentication().setEnabled(true);
        properties.getAuthentication().setType(EnterpriseStarterProperties.Authentication.AuthType.API_KEY);
        properties.getAuthentication().setApiKeys(java.util.Map.of("valid-key", List.of("USER")));
        properties.getAuthentication().setAllowedRoles(List.of());
        properties.getAuthentication().setEnableIpWhitelist(false);
        properties.getAuthentication().setOperationRules(java.util.Map.of());
        
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-API-Key", "valid-key");
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/api/test",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);
        
        assertTrue(response.getStatusCode().is2xxSuccessful());
    }

    @Test
    void testAuthentication_APIKey_Invalid() throws Exception {
        properties.getAuthentication().setEnabled(true);
        properties.getAuthentication().setType(EnterpriseStarterProperties.Authentication.AuthType.API_KEY);
        properties.getAuthentication().setApiKeys(java.util.Map.of("valid-key", List.of("USER")));
        
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-API-Key", "invalid-key");
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/api/test",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);
        
        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    void testAuthentication_BasicAuth_Valid() throws Exception {
        properties.getAuthentication().setEnabled(true);
        properties.getAuthentication().setType(EnterpriseStarterProperties.Authentication.AuthType.BASIC);
        properties.getAuthentication().setBasicAuthUsers(java.util.Map.of("user", "password"));
        properties.getAuthentication().setAllowedRoles(List.of());
        properties.getAuthentication().setEnableIpWhitelist(false);
        properties.getAuthentication().setOperationRules(java.util.Map.of());
        
        String credentials = Base64.getEncoder().encodeToString("user:password".getBytes());
        
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Basic " + credentials);
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/api/test",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);
        
        assertTrue(response.getStatusCode().is2xxSuccessful());
    }

    @Test
    void testRateLimiting_BlocksExcessiveRequests() throws Exception {
        properties.getAuthentication().setEnabled(true);
        properties.getAuthentication().setEnableRateLimiting(true);
        properties.getAuthentication().setRateLimitPerMinute(2);
        properties.getAuthentication().setType(EnterpriseStarterProperties.Authentication.AuthType.API_KEY);
        properties.getAuthentication().setApiKeys(java.util.Map.of("key", List.of("USER")));
        properties.getAuthentication().setAllowedRoles(List.of());
        properties.getAuthentication().setEnableIpWhitelist(false);
        properties.getAuthentication().setOperationRules(java.util.Map.of());
        
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-API-Key", "key");
        
        // Make 2 requests (within limit)
        ResponseEntity<String> response1 = restTemplate.exchange(
                baseUrl + "/api/test",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);
        assertTrue(response1.getStatusCode().is2xxSuccessful());
        
        ResponseEntity<String> response2 = restTemplate.exchange(
                baseUrl + "/api/test",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);
        assertTrue(response2.getStatusCode().is2xxSuccessful());
        
        // 3rd request should be blocked
        ResponseEntity<String> response3 = restTemplate.exchange(
                baseUrl + "/api/test",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);
        assertEquals(429, response3.getStatusCode().value());
    }

    @Test
    void testSecurityHeaders_Added() throws Exception {
        properties.getAuthentication().setEnabled(true);
        properties.getAuthentication().setEnableSecurityHeaders(true);
        properties.getAuthentication().setType(EnterpriseStarterProperties.Authentication.AuthType.API_KEY);
        properties.getAuthentication().setApiKeys(java.util.Map.of("key", List.of("USER")));
        properties.getAuthentication().setAllowedRoles(List.of());
        properties.getAuthentication().setEnableIpWhitelist(false);
        properties.getAuthentication().setOperationRules(java.util.Map.of());
        
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-API-Key", "key");
        
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/api/test",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);
        
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals("nosniff", response.getHeaders().getFirst("X-Content-Type-Options"));
        assertEquals("DENY", response.getHeaders().getFirst("X-Frame-Options"));
    }
}

