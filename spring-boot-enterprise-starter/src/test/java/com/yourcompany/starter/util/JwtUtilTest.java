package com.yourcompany.starter.util;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private String secretKey;
    private long validitySeconds;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        secretKey = "mySecretKey123456789012345678901234567890"; // 32+ chars for HS256
        validitySeconds = 3600;
    }

    @Test
    void testGenerateToken() {
        String username = "testuser";
        List<String> roles = List.of("USER", "ADMIN");
        
        String token = jwtUtil.generateToken(username, roles, secretKey, validitySeconds);
        
        assertNotNull(token);
        assertFalse(token.isEmpty());
        // JWT format: header.payload.signature (3 parts separated by dots)
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length);
    }

    @Test
    void testExtractUsername() {
        String username = "testuser";
        String token = jwtUtil.generateToken(username, List.of("USER"), secretKey, validitySeconds);
        
        String extractedUsername = jwtUtil.extractUsername(token, secretKey);
        
        assertEquals(username, extractedUsername);
    }

    @Test
    void testExtractRoles() {
        List<String> roles = List.of("USER", "ADMIN", "MANAGER");
        String token = jwtUtil.generateToken("testuser", roles, secretKey, validitySeconds);
        
        List<String> extractedRoles = jwtUtil.extractRoles(token, secretKey);
        
        assertNotNull(extractedRoles);
        assertEquals(3, extractedRoles.size());
        assertTrue(extractedRoles.containsAll(roles));
    }

    @Test
    void testValidateToken_ValidToken() {
        String token = jwtUtil.generateToken("testuser", List.of("USER"), secretKey, validitySeconds);
        
        Boolean isValid = jwtUtil.validateToken(token, secretKey);
        
        assertTrue(isValid);
    }

    @Test
    void testValidateToken_InvalidSecret() {
        String token = jwtUtil.generateToken("testuser", List.of("USER"), secretKey, validitySeconds);
        String wrongSecret = "wrongSecretKey123456789012345678901234567890";
        
        Boolean isValid = jwtUtil.validateToken(token, wrongSecret);
        
        assertFalse(isValid);
    }

    @Test
    void testValidateToken_ExpiredToken() throws InterruptedException {
        // Generate token with very short validity (1 second)
        String token = jwtUtil.generateToken("testuser", List.of("USER"), secretKey, 1);
        
        // Wait for token to expire
        Thread.sleep(2000);
        
        Boolean isValid = jwtUtil.validateToken(token, secretKey);
        
        assertFalse(isValid);
    }

    @Test
    void testExtractExpiration() {
        String token = jwtUtil.generateToken("testuser", List.of("USER"), secretKey, validitySeconds);
        
        Date expiration = jwtUtil.extractExpiration(token, secretKey);
        
        assertNotNull(expiration);
        assertTrue(expiration.after(new Date()));
    }

    @Test
    void testExtractAllClaims() {
        String token = jwtUtil.generateToken("testuser", List.of("USER", "ADMIN"), secretKey, validitySeconds);
        
        Claims claims = jwtUtil.extractAllClaims(token, secretKey);
        
        assertNotNull(claims);
        assertEquals("testuser", claims.getSubject());
        assertNotNull(claims.get("roles"));
    }

    @Test
    void testExtractRoles_NoRoles() {
        String token = jwtUtil.generateToken("testuser", List.of(), secretKey, validitySeconds);
        
        List<String> roles = jwtUtil.extractRoles(token, secretKey);
        
        assertNotNull(roles);
        assertTrue(roles.isEmpty());
    }
}

