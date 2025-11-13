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
 * 
 * This ensures that Redis (L2) is properly populated and can be shared across instances.
 * If Redis becomes unavailable, the cache continues to work with L1 (Caffeine) only,
 * providing resilience and fault tolerance.
 */
public class MultiLevelCacheManager implements CacheManager {

    private static final Logger logger = LoggerFactory.getLogger(MultiLevelCacheManager.class);

    private final CacheManager l1CacheManager;  // Caffeine (fast, local)
    private final CacheManager l2CacheManager;  // Redis (distributed)
    private final ConcurrentMap<String, Cache> cacheMap = new ConcurrentHashMap<>();
    private final Set<String> cacheNames = new LinkedHashSet<>();

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
     * Multi-level cache wrapper that coordinates between L1 and L2.
     */
    private static class MultiLevelCache implements Cache {
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
            
            // Check L2 (Redis) - handle failures gracefully
            if (l2Cache != null) {
                try {
                    ValueWrapper wrapper = l2Cache.get(key);
                    if (wrapper != null) {
                        // Populate L1 from L2 (cache promotion)
                        if (l1Cache != null) {
                            l1Cache.put(key, wrapper.get());
                        }
                        return wrapper;
                    }
                } catch (Exception e) {
                    logger.warn("Error reading from L2 cache (Redis) for key '{}' in cache '{}': {}. " +
                            "Falling back to L1 only.", key, name, e.getMessage());
                }
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
            
            // Check L2 - handle failures gracefully
            if (l2Cache != null) {
                try {
                    T value = l2Cache.get(key, type);
                    if (value != null) {
                        // Populate L1 from L2
                        if (l1Cache != null) {
                            l1Cache.put(key, value);
                        }
                        return value;
                    }
                } catch (Exception e) {
                    logger.warn("Error reading from L2 cache (Redis) for key '{}' in cache '{}': {}. " +
                            "Falling back to L1 only.", key, name, e.getMessage());
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
            
            // Check L2 - handle failures gracefully
            if (l2Cache != null) {
                try {
                    ValueWrapper wrapper = l2Cache.get(key);
                    if (wrapper != null) {
                        @SuppressWarnings("unchecked")
                        T value = (T) wrapper.get();
                        // Populate L1 from L2
                        if (l1Cache != null) {
                            l1Cache.put(key, value);
                        }
                        return value;
                    }
                } catch (Exception e) {
                    logger.warn("Error reading from L2 cache (Redis) for key '{}' in cache '{}': {}. " +
                            "Falling back to L1 only.", key, name, e.getMessage());
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
            
            // Write to L2 (handle failures gracefully)
            if (l2Cache != null) {
                try {
                    l2Cache.put(key, value);
                } catch (Exception e) {
                    logger.warn("Error writing to L2 cache (Redis) for key '{}' in cache '{}': {}. " +
                            "Value stored in L1 only.", key, name, e.getMessage());
                }
            }
        }

        @Override
        public void evict(Object key) {
            // Evict from L1 (always succeeds)
            if (l1Cache != null) {
                l1Cache.evict(key);
            }
            
            // Evict from L2 (handle failures gracefully)
            if (l2Cache != null) {
                try {
                    l2Cache.evict(key);
                } catch (Exception e) {
                    logger.warn("Error evicting from L2 cache (Redis) for key '{}' in cache '{}': {}. " +
                            "Key evicted from L1 only.", key, name, e.getMessage());
                }
            }
        }

        @Override
        public void clear() {
            // Clear L1 (always succeeds)
            if (l1Cache != null) {
                l1Cache.clear();
            }
            
            // Clear L2 (handle failures gracefully)
            if (l2Cache != null) {
                try {
                    l2Cache.clear();
                } catch (Exception e) {
                    logger.warn("Error clearing L2 cache (Redis) for cache '{}': {}. " +
                            "L1 cache cleared only.", name, e.getMessage());
                }
            }
        }
    }
}

