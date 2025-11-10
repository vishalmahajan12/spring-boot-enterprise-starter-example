package com.yourcompany.sample.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Service demonstrating multi-level caching features.
 * 
 * This service shows:
 * - @Cacheable - Cache method results
 * - @CachePut - Update cache when data changes
 * - @CacheEvict - Remove from cache
 * - Multi-level caching (L1: Caffeine, L2: Redis if enabled)
 */
@Service
public class CachedDataService {

    private static final Logger logger = LoggerFactory.getLogger(CachedDataService.class);
    
    // Simulated database/store
    private final Map<String, String> dataStore = new HashMap<>();

    public CachedDataService() {
        // Initialize with some sample data
        dataStore.put("config1", "Configuration Value 1");
        dataStore.put("config2", "Configuration Value 2");
    }

    /**
     * Example: Cache configuration data.
     * Uses L1 (Caffeine) first, then L2 (Redis) if available.
     * 
     * First call: Executes method and stores in cache
     * Subsequent calls: Returns from cache (very fast)
     */
    @Cacheable(value = "config", key = "#key")
    public String getConfig(String key) {
        logger.info("Fetching config from source (cache miss): {}", key);
        // Simulate database/API call
        try {
            Thread.sleep(100); // Simulate slow operation
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return dataStore.getOrDefault(key, "Default Value for " + key);
    }

    /**
     * Example: Cache user data.
     * Automatically uses multi-level cache (L1 + L2).
     */
    @Cacheable(value = "users", key = "#id")
    public Map<String, Object> getUser(Long id) {
        logger.info("Fetching user from source (cache miss): {}", id);
        // Simulate database call
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        Map<String, Object> user = new HashMap<>();
        user.put("id", id);
        user.put("name", "User " + id);
        user.put("email", "user" + id + "@example.com");
        return user;
    }

    /**
     * Example: Update cache when data changes.
     * @CachePut updates the cache with the new value.
     */
    @CachePut(value = "config", key = "#key")
    public String updateConfig(String key, String value) {
        logger.info("Updating config: {} = {}", key, value);
        dataStore.put(key, value);
        // Cache is automatically updated with the return value
        return value;
    }

    /**
     * Example: Update user cache.
     */
    @CachePut(value = "users", key = "#user.get('id')")
    public Map<String, Object> updateUser(Map<String, Object> user) {
        logger.info("Updating user: {}", user.get("id"));
        // Simulate database update
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        // Cache is automatically updated
        return user;
    }

    /**
     * Example: Remove specific item from cache.
     * @CacheEvict removes the item from both L1 and L2 caches.
     */
    @CacheEvict(value = "config", key = "#key")
    public void evictConfig(String key) {
        logger.info("Evicting config from cache: {}", key);
        dataStore.remove(key);
        // Cache entry is automatically removed
    }

    /**
     * Example: Remove user from cache.
     */
    @CacheEvict(value = "users", key = "#id")
    public void evictUser(Long id) {
        logger.info("Evicting user from cache: {}", id);
        // Cache entry is automatically removed
    }

    /**
     * Example: Clear entire cache.
     * allEntries = true clears all entries in the cache.
     */
    @CacheEvict(value = "config", allEntries = true)
    public void clearConfigCache() {
        logger.info("Clearing all config cache entries");
        // All cache entries are automatically removed
    }

    /**
     * Example: Cache external API response.
     * Useful for caching expensive external API calls.
     */
    @Cacheable(value = "api-responses", key = "#endpoint")
    public String getCachedApiResponse(String endpoint) {
        logger.info("Calling external API (cache miss): {}", endpoint);
        // Simulate external API call
        try {
            Thread.sleep(200); // Simulate network latency
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "Response from " + endpoint + " at " + System.currentTimeMillis();
    }

    /**
     * Example: Method that doesn't use caching.
     * Useful for comparison - this will always execute.
     */
    public String getUncachedData(String key) {
        logger.info("Fetching uncached data: {}", key);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return dataStore.getOrDefault(key, "Uncached Value");
    }
}

