package com.yourcompany.starter.service;

import com.yourcompany.starter.config.EnterpriseStarterProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitingServiceTest {

    private RateLimitingService rateLimitingService;
    private EnterpriseStarterProperties properties;

    @BeforeEach
    void setUp() {
        properties = new EnterpriseStarterProperties();
        properties.getAuthentication().setEnableRateLimiting(true);
        properties.getAuthentication().setRateLimitPerMinute(5);
        
        rateLimitingService = new RateLimitingService(properties);
    }

    @Test
    void testIsAllowed_WithinLimit() {
        String identifier = "test-ip-1";
        
        // Make 5 requests (within limit)
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimitingService.isAllowed(identifier), 
                    "Request " + (i + 1) + " should be allowed");
        }
    }

    @Test
    void testIsAllowed_ExceedsLimit() {
        String identifier = "test-ip-2";
        
        // Make 5 requests (within limit)
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimitingService.isAllowed(identifier));
        }
        
        // 6th request should be blocked
        assertFalse(rateLimitingService.isAllowed(identifier), 
                "6th request should be blocked");
    }

    @Test
    void testIsAllowed_DifferentIdentifiers() {
        String id1 = "ip-1";
        String id2 = "ip-2";
        
        // Both should be allowed independently
        assertTrue(rateLimitingService.isAllowed(id1));
        assertTrue(rateLimitingService.isAllowed(id2));
        
        // Exhaust limit for id1
        for (int i = 0; i < 4; i++) {
            rateLimitingService.isAllowed(id1);
        }
        
        // id1 should be blocked, id2 should still be allowed
        assertFalse(rateLimitingService.isAllowed(id1));
        assertTrue(rateLimitingService.isAllowed(id2));
    }

    @Test
    void testGetCurrentCount() {
        String identifier = "test-ip-3";
        
        assertEquals(0, rateLimitingService.getCurrentCount(identifier));
        
        rateLimitingService.isAllowed(identifier);
        assertEquals(1, rateLimitingService.getCurrentCount(identifier));
        
        rateLimitingService.isAllowed(identifier);
        assertEquals(2, rateLimitingService.getCurrentCount(identifier));
    }

    @Test
    void testIsAllowed_RateLimitingDisabled() {
        properties.getAuthentication().setEnableRateLimiting(false);
        RateLimitingService service = new RateLimitingService(properties);
        
        // Should always allow when disabled
        for (int i = 0; i < 100; i++) {
            assertTrue(service.isAllowed("any-id"));
        }
    }

    @Test
    void testIsAllowed_ZeroLimit() {
        properties.getAuthentication().setRateLimitPerMinute(0);
        RateLimitingService service = new RateLimitingService(properties);
        
        // Zero limit should always allow
        assertTrue(service.isAllowed("test-id"));
    }
}

