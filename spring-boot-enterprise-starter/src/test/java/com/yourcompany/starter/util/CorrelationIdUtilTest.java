package com.yourcompany.starter.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CorrelationIdUtilTest {

    @BeforeEach
    @AfterEach
    void cleanup() {
        CorrelationIdUtil.clear();
    }

    @Test
    void testGenerateCorrelationId() {
        String correlationId = CorrelationIdUtil.generateCorrelationId();
        
        assertNotNull(correlationId);
        assertFalse(correlationId.isEmpty());
        // UUID format: 8-4-4-4-12 hex digits
        assertTrue(correlationId.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
    }

    @Test
    void testSetAndGetCorrelationId() {
        String testId = "test-correlation-id-123";
        CorrelationIdUtil.setCorrelationId(testId);
        
        String retrievedId = CorrelationIdUtil.getCorrelationId();
        assertEquals(testId, retrievedId);
    }

    @Test
    void testGetCorrelationIdGeneratesNewWhenNotSet() {
        CorrelationIdUtil.clear();
        
        String correlationId = CorrelationIdUtil.getCorrelationId();
        
        assertNotNull(correlationId);
        assertFalse(correlationId.isEmpty());
        // Should have generated a new one
        assertTrue(correlationId.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
    }

    @Test
    void testClearCorrelationId() {
        CorrelationIdUtil.setCorrelationId("test-id");
        CorrelationIdUtil.clear();
        
        // After clear, getCorrelationId should generate a new one
        String newId = CorrelationIdUtil.getCorrelationId();
        assertNotEquals("test-id", newId);
        assertNotNull(newId);
    }

    @Test
    void testThreadLocalIsolation() throws InterruptedException {
        String id1 = "thread-1-id";
        String id2 = "thread-2-id";
        
        Thread thread1 = new Thread(() -> {
            CorrelationIdUtil.setCorrelationId(id1);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            assertEquals(id1, CorrelationIdUtil.getCorrelationId());
        });
        
        Thread thread2 = new Thread(() -> {
            CorrelationIdUtil.setCorrelationId(id2);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            assertEquals(id2, CorrelationIdUtil.getCorrelationId());
        });
        
        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();
        
        // Main thread should not have these IDs
        assertNotEquals(id1, CorrelationIdUtil.getCorrelationId());
        assertNotEquals(id2, CorrelationIdUtil.getCorrelationId());
    }
}

