package com.yourcompany.starter.config;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.CompositeCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redis cache configuration (L2).
 * This configuration is only loaded when Redis classes are available on the classpath.
 * 
 * Uses string-based class names to avoid compile-time dependencies on Redis.
 */
@Configuration
@ConditionalOnClass(name = "org.springframework.data.redis.connection.RedisConnectionFactory")
@ConditionalOnProperty(prefix = "enterprise.starter.cache", name = "redis-enabled", havingValue = "true", matchIfMissing = true)
public class RedisCacheConfig {

    private final EnterpriseStarterProperties properties;

    public RedisCacheConfig(EnterpriseStarterProperties properties) {
        this.properties = properties;
    }

    /**
     * L2 Cache Manager (Redis) - Distributed cache.
     * Only created if Redis is enabled and RedisConnectionFactory is available.
     */
    @Bean
    @ConditionalOnBean(name = "redisConnectionFactory")
    public CacheManager redisCacheManager(Object redisConnectionFactory) {
        EnterpriseStarterProperties.Cache cacheConfig = properties.getCache();
        
        try {
            // Load Redis classes dynamically
            Class<?> redisConnectionFactoryClass = Class.forName("org.springframework.data.redis.connection.RedisConnectionFactory");
            Class<?> redisCacheConfigurationClass = Class.forName("org.springframework.data.redis.cache.RedisCacheConfiguration");
            Class<?> redisCacheManagerClass = Class.forName("org.springframework.data.redis.cache.RedisCacheManager");
            Class<?> redisSerializationContextClass = Class.forName("org.springframework.data.redis.serializer.RedisSerializationContext");
            Class<?> stringRedisSerializerClass = Class.forName("org.springframework.data.redis.serializer.StringRedisSerializer");
            Class<?> genericJackson2JsonRedisSerializerClass = Class.forName("org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer");
            Class<?> redisSerializerClass = Class.forName("org.springframework.data.redis.serializer.RedisSerializer");
            
            // Create default configuration
            Object defaultConfig = redisCacheConfigurationClass.getMethod("defaultCacheConfig").invoke(null);
            defaultConfig = redisCacheConfigurationClass.getMethod("entryTtl", Duration.class)
                    .invoke(defaultConfig, Duration.ofSeconds(cacheConfig.getRedisTtlSeconds()));
            
            // Create serializers
            Object keySerializer = stringRedisSerializerClass.getConstructor().newInstance();
            Object valueSerializer = genericJackson2JsonRedisSerializerClass.getConstructor().newInstance();
            
            // Configure serialization
            Object keyPair = redisSerializationContextClass.getMethod("fromSerializer", redisSerializerClass).invoke(null, keySerializer);
            Object valuePair = redisSerializationContextClass.getMethod("fromSerializer", redisSerializerClass).invoke(null, valueSerializer);
            
            // Get SerializationPair class
            Class<?> serializationPairClass = Arrays.stream(redisSerializationContextClass.getClasses())
                    .filter(c -> c.getSimpleName().equals("SerializationPair"))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("SerializationPair not found"));
            
            defaultConfig = redisCacheConfigurationClass.getMethod("serializeKeysWith", serializationPairClass)
                    .invoke(defaultConfig, keyPair);
            defaultConfig = redisCacheConfigurationClass.getMethod("serializeValuesWith", serializationPairClass)
                    .invoke(defaultConfig, valuePair);
            defaultConfig = redisCacheConfigurationClass.getMethod("disableCachingNullValues").invoke(defaultConfig);
            
            // Cache-specific configurations
            Map<String, Object> cacheConfigurations = new HashMap<>();
            Object tokensConfig = redisCacheConfigurationClass.getMethod("entryTtl", Duration.class)
                    .invoke(defaultConfig, Duration.ofMinutes(15));
            Object configConfig = redisCacheConfigurationClass.getMethod("entryTtl", Duration.class)
                    .invoke(defaultConfig, Duration.ofHours(24));
            
            cacheConfigurations.put("tokens", tokensConfig);
            cacheConfigurations.put("config", configConfig);
            cacheConfigurations.put("default", defaultConfig);
            
            // Add other cache names
            for (String cacheName : cacheConfig.getCacheNames()) {
                if (!cacheConfigurations.containsKey(cacheName)) {
                    cacheConfigurations.put(cacheName, defaultConfig);
                }
            }
            
            // Build RedisCacheManager
            Object builder = redisCacheManagerClass.getMethod("builder", redisConnectionFactoryClass)
                    .invoke(null, redisConnectionFactory);
            builder = redisCacheManagerClass.getMethod("cacheDefaults", redisCacheConfigurationClass).invoke(builder, defaultConfig);
            builder = redisCacheManagerClass.getMethod("withInitialCacheConfigurations", Map.class).invoke(builder, cacheConfigurations);
            builder = redisCacheManagerClass.getMethod("transactionAware").invoke(builder);
            return (CacheManager) redisCacheManagerClass.getMethod("build").invoke(builder);
            
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create Redis cache manager. Make sure spring-boot-starter-data-redis is on the classpath.", e);
        }
    }

    /**
     * Composite Cache Manager - Combines L1 and L2.
     * Checks Caffeine first, then Redis, then source.
     */
    @Bean
    @ConditionalOnBean(name = "redisCacheManager")
    public CacheManager compositeCacheManager(CacheManager caffeineCacheManager,
                                              CacheManager redisCacheManager) {
        CompositeCacheManager compositeCacheManager = new CompositeCacheManager();
        compositeCacheManager.setCacheManagers(Arrays.asList(
                caffeineCacheManager,  // Check L1 first (fastest)
                redisCacheManager     // Then L2 (distributed)
        ));
        compositeCacheManager.setFallbackToNoOpCache(false);
        return compositeCacheManager;
    }
}

