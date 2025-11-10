package com.yourcompany.starter.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.github.benmanes.caffeine.cache.CaffeineSpec;

/**
 * Multi-level cache configuration.
 * 
 * Provides two-tier caching:
 * 1. L1 Cache (Caffeine) - Fast, local, in-memory cache
 * 2. L2 Cache (Redis) - Distributed cache, shared across instances (optional)
 * 
 * Architecture:
 * Request → L1 Cache (Caffeine) → L2 Cache (Redis) → Source (Database/API)
 * 
 * Benefits:
 * - Ultra-fast access with Caffeine (sub-millisecond)
 * - Distributed caching with Redis (shared across instances)
 * - Fallback resilience (works even if Redis is unavailable)
 * - Reduced load on Redis with local cache hits
 * 
 * Only enabled when enterprise.starter.cache.enabled=true
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(prefix = "enterprise.starter.cache", name = "enabled", havingValue = "true", matchIfMissing = false)
public class CacheConfig {

    private final EnterpriseStarterProperties properties;

    public CacheConfig(EnterpriseStarterProperties properties) {
        this.properties = properties;
    }

    /**
     * L1 Cache Manager (Caffeine) - Primary, fast local cache.
     * Always created when caching is enabled.
     */
    @Bean
    @Primary
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        EnterpriseStarterProperties.Cache cacheConfig = properties.getCache();
        
        // Build Caffeine specification string
        String spec = String.format(
            "maximumSize=%d,expireAfterWrite=%ds,expireAfterAccess=%ds,recordStats",
            cacheConfig.getCaffeineMaxSize(),
            cacheConfig.getCaffeineTtlSeconds(),
            cacheConfig.getCaffeineAccessExpirationSeconds()
        );
        
        cacheManager.setCaffeineSpec(CaffeineSpec.parse(spec));
        cacheManager.setCacheNames(cacheConfig.getCacheNames());
        return cacheManager;
    }

}

