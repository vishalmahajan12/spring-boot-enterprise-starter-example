package com.yourcompany.starter.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MaskingUtilTest {

    @Test
    void testMaskSensitiveData_JsonPassword() {
        String json = "{\"username\":\"test\",\"password\":\"secret123\"}";
        List<String> sensitiveFields = Arrays.asList("password", "token");
        
        String masked = MaskingUtil.maskSensitiveData(json, sensitiveFields);
        
        assertNotNull(masked);
        assertTrue(masked.contains("****"));
        assertFalse(masked.contains("secret123"));
        assertTrue(masked.contains("test")); // Non-sensitive field should remain
    }

    @Test
    void testMaskSensitiveData_JsonToken() {
        String json = "{\"user\":\"john\",\"token\":\"abc123xyz\"}";
        List<String> sensitiveFields = Arrays.asList("token");
        
        String masked = MaskingUtil.maskSensitiveData(json, sensitiveFields);
        
        assertTrue(masked.contains("****"));
        assertFalse(masked.contains("abc123xyz"));
    }

    @Test
    void testMaskSensitiveData_NestedJson() {
        String json = "{\"user\":{\"name\":\"john\",\"password\":\"pass123\"},\"token\":\"token123\"}";
        List<String> sensitiveFields = Arrays.asList("password", "token");
        
        String masked = MaskingUtil.maskSensitiveData(json, sensitiveFields);
        
        assertTrue(masked.contains("****"));
        assertFalse(masked.contains("pass123"));
        assertFalse(masked.contains("token123"));
    }

    @Test
    void testMaskSensitiveData_ArrayJson() {
        String json = "[{\"name\":\"user1\",\"password\":\"pass1\"},{\"name\":\"user2\",\"password\":\"pass2\"}]";
        List<String> sensitiveFields = Arrays.asList("password");
        
        String masked = MaskingUtil.maskSensitiveData(json, sensitiveFields);
        
        assertTrue(masked.contains("****"));
        assertFalse(masked.contains("pass1"));
        assertFalse(masked.contains("pass2"));
    }

    @Test
    void testMaskSensitiveData_NonJson() {
        String text = "This is plain text with password: secret123";
        List<String> sensitiveFields = Arrays.asList("password");
        
        String masked = MaskingUtil.maskSensitiveData(text, sensitiveFields);
        
        // Should try regex masking for patterns
        assertNotNull(masked);
    }

    @Test
    void testMaskSensitiveData_NullInput() {
        String masked = MaskingUtil.maskSensitiveData(null, Arrays.asList("password"));
        assertNull(masked);
    }

    @Test
    void testMaskSensitiveData_EmptySensitiveFields() {
        String json = "{\"password\":\"secret123\"}";
        String masked = MaskingUtil.maskSensitiveData(json, List.of());
        
        assertEquals(json, masked); // Should not mask if no sensitive fields
    }

    @Test
    void testMaskHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer token123");
        headers.put("Content-Type", "application/json");
        headers.put("X-API-Key", "api-key-123");
        
        List<String> sensitiveFields = Arrays.asList("authorization", "api-key");
        
        Map<String, String> masked = MaskingUtil.maskHeaders(headers, sensitiveFields);
        
        assertEquals("****", masked.get("Authorization"));
        assertEquals("****", masked.get("X-API-Key"));
        assertEquals("application/json", masked.get("Content-Type")); // Non-sensitive should remain
    }

    @Test
    void testMaskHeaders_CaseInsensitive() {
        Map<String, String> headers = new HashMap<>();
        headers.put("authorization", "Bearer token123");
        headers.put("AUTHORIZATION", "Bearer token456");
        
        Map<String, String> masked = MaskingUtil.maskHeaders(headers, Arrays.asList("authorization"));
        
        assertEquals("****", masked.get("authorization"));
        assertEquals("****", masked.get("AUTHORIZATION"));
    }

    @Test
    void testMaskHeaders_NullHeaders() {
        Map<String, String> masked = MaskingUtil.maskHeaders(null, Arrays.asList("password"));
        assertNull(masked);
    }
}

