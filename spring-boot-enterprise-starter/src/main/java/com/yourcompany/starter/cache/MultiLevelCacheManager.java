package com.yourcompany.starter.cache;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

/**
 * Multi-level cache manager that properly writes to both L1 and L2 caches.
 * 
 * Unlike CompositeCacheManager, this implementation:
 * - Reads from L1 first, then L2, then source
 * - Writes to BOTH L1 and L2 on cache miss (write-through)
 * - Evicts from BOTH L1 and L2 on eviction
 * - Handles L2 (Redis) failures gracefully, falling back to L1 only
 * - Uses circuit breaker pattern to fast-fail when Redis is down
 * 
 * This ensures that Redis (L2) is properly populated and can be shared across instances.
 * If Redis becomes unavailable, the cache continues to work with L1 (Caffeine) only,
 * providing resilience and fault tolerance. The circuit breaker prevents slow requests
 * by skipping Redis after detecting failures.
 */
public class MultiLevelCacheManager implements CacheManager {

    private static final Logger logger = LoggerFactory.getLogger(MultiLevelCacheManager.class);

    private final CacheManager l1CacheManager;  // Caffeine (fast, local)
    private final CacheManager l2CacheManager;  // Redis (distributed)
    private final ConcurrentMap<String, Cache> cacheMap = new ConcurrentHashMap<>();
    private final Set<String> cacheNames = new LinkedHashSet<>();
    private final CircuitBreaker l2CircuitBreaker = new CircuitBreaker();

    public MultiLevelCacheManager(CacheManager l1CacheManager, CacheManager l2CacheManager) {
        if (l1CacheManager == null && l2CacheManager == null) {
            throw new IllegalArgumentException("At least one cache manager (L1 or L2) must be provided");
        }
        this.l1CacheManager = l1CacheManager;
        this.l2CacheManager = l2CacheManager;
        
        // Collect all cache names from both managers
        Set<String> names = new LinkedHashSet<>();
        if (l1CacheManager != null) {
            names.addAll(l1CacheManager.getCacheNames());
        }
        if (l2CacheManager != null) {
            names.addAll(l2CacheManager.getCacheNames());
        }
        this.cacheNames.addAll(names);
    }

    @Override
    public Cache getCache(String name) {
        Cache cache = cacheMap.get(name);
        if (cache != null) {
            return cache;
        }
        
        // Create multi-level cache wrapper
        Cache l1Cache = l1CacheManager != null ? l1CacheManager.getCache(name) : null;
        Cache l2Cache = l2CacheManager != null ? l2CacheManager.getCache(name) : null;
        
        if (l1Cache == null && l2Cache == null) {
            return null;
        }
        
        cache = new MultiLevelCache(name, l1Cache, l2Cache);
        Cache existing = cacheMap.putIfAbsent(name, cache);
        return existing != null ? existing : cache;
    }

    @Override
    public Collection<String> getCacheNames() {
        return Collections.unmodifiableSet(cacheNames);
    }

    /**
     * Circuit breaker to prevent slow requests when Redis is down.
     * After N consecutive failures, skips Redis for a period of time.
     */
    private class CircuitBreaker {
        private volatile boolean isOpen = false;
        private volatile long lastFailureTime = 0;
        private volatile int failureCount = 0;
        private static final int FAILURE_THRESHOLD = 5;
        private static final long CIRCUIT_OPEN_DURATION_MS = 30000; // 30 seconds
        
        /**
         * Returns true if we should attempt to use L2 cache (Redis).
         * Returns false if circuit is open (Redis is down).
         */
        boolean shouldAttempt() {
            if (!isOpen) {
                return true;
            }
            // Check if we should try again (half-open state after timeout)
            if (System.currentTimeMillis() - lastFailureTime > CIRCUIT_OPEN_DURATION_MS) {
                isOpen = false;
                failureCount = 0;
                logger.info("Circuit breaker: Attempting to reconnect to Redis (half-open state)");
                return true;
            }
            return false;
        }
        
        /**
         * Records a successful operation, resetting the circuit breaker.
         */
        void recordSuccess() {
            if (isOpen || failureCount > 0) {
                logger.info("Circuit breaker: Redis is healthy again. Resetting circuit breaker.");
            }
            isOpen = false;
            failureCount = 0;
        }
        
        /**
         * Records a failure. Opens circuit after threshold is reached.
         */
        void recordFailure() {
            failureCount++;
            lastFailureTime = System.currentTimeMillis();
            if (failureCount >= FAILURE_THRESHOLD && !isOpen) {
                isOpen = true;
                logger.warn("Circuit breaker: OPENED after {} failures. Skipping Redis for {} seconds.", 
                        failureCount, CIRCUIT_OPEN_DURATION_MS / 1000);
            }
        }
        
        boolean isOpen() {
            return isOpen;
        }
        
        int getFailureCount() {
            return failureCount;
        }
    }

    /**
     * Multi-level cache wrapper that coordinates between L1 and L2.
     */
    private class MultiLevelCache implements Cache {
        private final String name;
        private final Cache l1Cache;
        private final Cache l2Cache;

        public MultiLevelCache(String name, Cache l1Cache, Cache l2Cache) {
            this.name = name;
            this.l1Cache = l1Cache;
            this.l2Cache = l2Cache;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Object getNativeCache() {
            return l1Cache != null ? l1Cache.getNativeCache() : 
                   l2Cache != null ? l2Cache.getNativeCache() : null;
        }

        @Override
        public ValueWrapper get(Object key) {
            // Check L1 first (fastest)
            if (l1Cache != null) {
                ValueWrapper wrapper = l1Cache.get(key);
                if (wrapper != null) {
                    return wrapper;
                }
            }
            
            // Check L2 (Redis) - only if circuit breaker allows (fast-fail when Redis is down)
            if (l2Cache != null && l2CircuitBreaker.shouldAttempt()) {
                try {
                    ValueWrapper wrapper = l2Cache.get(key);
                    if (wrapper != null) {
                        l2CircuitBreaker.recordSuccess(); // Reset on success
                        // Populate L1 from L2 (cache promotion)
                        if (l1Cache != null) {
                            l1Cache.put(key, wrapper.get());
                        }
                        return wrapper;
                    }
                    // Cache miss is not a failure - only record success if we got a value
                    if (l2CircuitBreaker.isOpen()) {
                        l2CircuitBreaker.recordSuccess(); // Successful connection even if miss
                    }
                } catch (Exception e) {
                    l2CircuitBreaker.recordFailure(); // Track failure
                    if (l2CircuitBreaker.isOpen()) {
                        logger.debug("Skipping L2 cache (Redis) - circuit breaker is OPEN for cache '{}'", name);
                    } else {
                        logger.warn("Error reading from L2 cache (Redis) for key '{}' in cache '{}': {}. " +
                                "Failure count: {}/{}", key, name, e.getMessage(), 
                                l2CircuitBreaker.getFailureCount(), CircuitBreaker.FAILURE_THRESHOLD);
                    }
                }
            } else if (l2Cache != null && !l2CircuitBreaker.shouldAttempt()) {
                // Circuit is open - skip Redis entirely (fast path, no delay)
                logger.trace("Skipping L2 cache (Redis) - circuit breaker is OPEN for cache '{}'", name);
            }
            
            return null;
        }

        @Override
        public <T> T get(Object key, Class<T> type) {
            // Check L1 first
            if (l1Cache != null) {
                T value = l1Cache.get(key, type);
                if (value != null) {
                    return value;
                }
            }
            
            // Check L2 - only if circuit breaker allows (fast-fail when Redis is down)
            if (l2Cache != null && l2CircuitBreaker.shouldAttempt()) {
                try {
                    T value = l2Cache.get(key, type);
                    if (value != null) {
                        l2CircuitBreaker.recordSuccess(); // Reset on success
                        // Populate L1 from L2
                        if (l1Cache != null) {
                            l1Cache.put(key, value);
                        }
                        return value;
                    }
                    // Cache miss is not a failure
                    if (l2CircuitBreaker.isOpen()) {
                        l2CircuitBreaker.recordSuccess(); // Successful connection even if miss
                    }
                } catch (Exception e) {
                    l2CircuitBreaker.recordFailure(); // Track failure
                    if (!l2CircuitBreaker.isOpen()) {
                        logger.warn("Error reading from L2 cache (Redis) for key '{}' in cache '{}': {}. " +
                                "Failure count: {}/{}", key, name, e.getMessage(),
                                l2CircuitBreaker.getFailureCount(), CircuitBreaker.FAILURE_THRESHOLD);
                    }
                }
            }
            
            return null;
        }

        @Override
        public <T> T get(Object key, Callable<T> valueLoader) {
            // Check L1 first
            if (l1Cache != null) {
                ValueWrapper wrapper = l1Cache.get(key);
                if (wrapper != null) {
                    @SuppressWarnings("unchecked")
                    T value = (T) wrapper.get();
                    // Also ensure it's in L2
                    if (l2Cache != null) {
                        l2Cache.put(key, value);
                    }
                    return value;
                }
            }
            
            // Check L2 - only if circuit breaker allows (fast-fail when Redis is down)
            if (l2Cache != null && l2CircuitBreaker.shouldAttempt()) {
                try {
                    ValueWrapper wrapper = l2Cache.get(key);
                    if (wrapper != null) {
                        l2CircuitBreaker.recordSuccess(); // Reset on success
                        @SuppressWarnings("unchecked")
                        T value = (T) wrapper.get();
                        // Populate L1 from L2
                        if (l1Cache != null) {
                            l1Cache.put(key, value);
                        }
                        return value;
                    }
                    // Cache miss is not a failure
                    if (l2CircuitBreaker.isOpen()) {
                        l2CircuitBreaker.recordSuccess(); // Successful connection even if miss
                    }
                } catch (Exception e) {
                    l2CircuitBreaker.recordFailure(); // Track failure
                    if (!l2CircuitBreaker.isOpen()) {
                        logger.warn("Error reading from L2 cache (Redis) for key '{}' in cache '{}': {}. " +
                                "Failure count: {}/{}", key, name, e.getMessage(),
                                l2CircuitBreaker.getFailureCount(), CircuitBreaker.FAILURE_THRESHOLD);
                    }
                }
            }
            
            // Not found in either cache, compute and store in both (write-through)
            try {
                T value = valueLoader.call();
                if (value != null) {
                    put(key, value);
                }
                return value;
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException("Error loading cache value for key: " + key + " in cache: " + name, e);
            }
        }

        @Override
        public void put(Object key, Object value) {
            // Write to L1 (always succeeds)
            if (l1Cache != null) {
                l1Cache.put(key, value);
            }
            
            // Write to L2 - only if circuit breaker allows (fast-fail when Redis is down)
            if (l2Cache != null && l2CircuitBreaker.shouldAttempt()) {
                try {
                    l2Cache.put(key, value);
                    l2CircuitBreaker.recordSuccess(); // Reset on success
                } catch (Exception e) {
                    l2CircuitBreaker.recordFailure(); // Track failure
                    if (!l2CircuitBreaker.isOpen()) {
                        logger.warn("Error writing to L2 cache (Redis) for key '{}' in cache '{}': {}. " +
                                "Value stored in L1 only. Failure count: {}/{}", 
                                key, name, e.getMessage(), l2CircuitBreaker.getFailureCount(), 
                                CircuitBreaker.FAILURE_THRESHOLD);
                    }
                }
            }
        }

        @Override
        public void evict(Object key) {
            // Evict from L1 (always succeeds)
            if (l1Cache != null) {
                l1Cache.evict(key);
            }
            
            // Evict from L2 - only if circuit breaker allows (fast-fail when Redis is down)
            if (l2Cache != null && l2CircuitBreaker.shouldAttempt()) {
                try {
                    l2Cache.evict(key);
                    l2CircuitBreaker.recordSuccess(); // Reset on success
                } catch (Exception e) {
                    l2CircuitBreaker.recordFailure(); // Track failure
                    if (!l2CircuitBreaker.isOpen()) {
                        logger.warn("Error evicting from L2 cache (Redis) for key '{}' in cache '{}': {}. " +
                                "Key evicted from L1 only. Failure count: {}/{}", 
                                key, name, e.getMessage(), l2CircuitBreaker.getFailureCount(),
                                CircuitBreaker.FAILURE_THRESHOLD);
                    }
                }
            }
        }

        @Override
        public void clear() {
            // Clear L1 (always succeeds)
            if (l1Cache != null) {
                l1Cache.clear();
            }
            
            // Clear L2 - only if circuit breaker allows (fast-fail when Redis is down)
            if (l2Cache != null && l2CircuitBreaker.shouldAttempt()) {
                try {
                    l2Cache.clear();
                    l2CircuitBreaker.recordSuccess(); // Reset on success
                } catch (Exception e) {
                    l2CircuitBreaker.recordFailure(); // Track failure
                    if (!l2CircuitBreaker.isOpen()) {
                        logger.warn("Error clearing L2 cache (Redis) for cache '{}': {}. " +
                                "L1 cache cleared only. Failure count: {}/{}", 
                                name, e.getMessage(), l2CircuitBreaker.getFailureCount(),
                                CircuitBreaker.FAILURE_THRESHOLD);
                    }
                }
            }
        }
    }
}

