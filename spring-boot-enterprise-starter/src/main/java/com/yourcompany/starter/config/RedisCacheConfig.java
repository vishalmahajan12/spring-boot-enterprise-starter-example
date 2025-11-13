package com.yourcompany.starter.config;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.yourcompany.starter.cache.MultiLevelCacheManager;

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

    private static final Logger logger = LoggerFactory.getLogger(RedisCacheConfig.class);
    
    private final EnterpriseStarterProperties properties;
    private final ApplicationContext applicationContext;

    public RedisCacheConfig(EnterpriseStarterProperties properties, ApplicationContext applicationContext) {
        this.properties = properties;
        this.applicationContext = applicationContext;
    }

    /**
     * L2 Cache Manager (Redis) - Distributed cache.
     * Only created if Redis is enabled and RedisConnectionFactory is available.
     * 
     * Note: We rely on Spring Boot's auto-configuration to create the redisConnectionFactory bean.
     */
    @Bean
    public CacheManager redisCacheManager() {
        EnterpriseStarterProperties.Cache cacheConfig = properties.getCache();
        
        try {
            // Load Redis classes dynamically (cached to avoid repeated lookups)
            RedisClasses redisClasses = loadRedisClasses();
            
            // Get RedisConnectionFactory bean (created by Spring Boot auto-configuration)
            Object redisConnectionFactory = getRedisConnectionFactory(redisClasses.redisConnectionFactoryClass);
            
            // Create default cache configuration
            Object defaultConfig = createDefaultCacheConfiguration(redisClasses.redisCacheConfigurationClass, cacheConfig);
            
            // Configure serialization
            defaultConfig = configureSerialization(defaultConfig, redisClasses);
            
            // Create cache-specific configurations
            Map<String, Object> cacheConfigurations = createCacheConfigurations(
                    defaultConfig, redisClasses.redisCacheConfigurationClass, cacheConfig);
            
            // Build RedisCacheManager
            CacheManager cacheManager = buildRedisCacheManager(
                    redisClasses, redisConnectionFactory, defaultConfig, cacheConfigurations);
            
            logger.info("Successfully created RedisCacheManager with {} cache(s)", cacheConfigurations.size());
            return cacheManager;
            
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Redis classes not found on classpath. " +
                    "Make sure spring-boot-starter-data-redis is included.", e);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create Redis cache manager: " + e.getMessage(), e);
        }
    }
    
    /**
     * Loads and caches all Redis-related classes to avoid repeated Class.forName() calls.
     */
    private RedisClasses loadRedisClasses() throws ClassNotFoundException {
        return new RedisClasses(
            Class.forName("org.springframework.data.redis.connection.RedisConnectionFactory"),
            Class.forName("org.springframework.data.redis.cache.RedisCacheConfiguration"),
            Class.forName("org.springframework.data.redis.cache.RedisCacheManager"),
            Class.forName("org.springframework.data.redis.serializer.RedisSerializationContext"),
            Class.forName("org.springframework.data.redis.serializer.StringRedisSerializer"),
            Class.forName("org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer"),
            Class.forName("org.springframework.data.redis.serializer.RedisSerializer")
        );
    }
    
    /**
     * Retrieves the RedisConnectionFactory bean from the application context.
     */
    private Object getRedisConnectionFactory(Class<?> redisConnectionFactoryClass) {
        try {
            return applicationContext.getBean("redisConnectionFactory");
        } catch (Exception e1) {
            try {
                return applicationContext.getBean(redisConnectionFactoryClass);
            } catch (Exception e2) {
                throw new IllegalStateException("RedisConnectionFactory bean not found. " +
                        "Cannot create Redis cache manager. Application will use Caffeine (L1) only.", e2);
            }
        }
    }
    
    /**
     * Holder class for Redis-related classes to avoid repeated Class.forName() calls.
     */
    private static class RedisClasses {
        final Class<?> redisConnectionFactoryClass;
        final Class<?> redisCacheConfigurationClass;
        final Class<?> redisCacheManagerClass;
        final Class<?> redisSerializationContextClass;
        final Class<?> stringRedisSerializerClass;
        final Class<?> genericJackson2JsonRedisSerializerClass;
        final Class<?> redisSerializerClass;
        Class<?> serializationPairClass; // Lazy-loaded
        
        RedisClasses(Class<?> redisConnectionFactoryClass, Class<?> redisCacheConfigurationClass,
                    Class<?> redisCacheManagerClass, Class<?> redisSerializationContextClass,
                    Class<?> stringRedisSerializerClass, Class<?> genericJackson2JsonRedisSerializerClass,
                    Class<?> redisSerializerClass) {
            this.redisConnectionFactoryClass = redisConnectionFactoryClass;
            this.redisCacheConfigurationClass = redisCacheConfigurationClass;
            this.redisCacheManagerClass = redisCacheManagerClass;
            this.redisSerializationContextClass = redisSerializationContextClass;
            this.stringRedisSerializerClass = stringRedisSerializerClass;
            this.genericJackson2JsonRedisSerializerClass = genericJackson2JsonRedisSerializerClass;
            this.redisSerializerClass = redisSerializerClass;
        }
        
        Class<?> getSerializationPairClass() {
            if (serializationPairClass == null) {
                serializationPairClass = Arrays.stream(redisSerializationContextClass.getClasses())
                        .filter(c -> c.getSimpleName().equals("SerializationPair"))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("SerializationPair not found"));
            }
            return serializationPairClass;
        }
    }
    
    /**
     * Creates the default Redis cache configuration with TTL.
     */
    private Object createDefaultCacheConfiguration(Class<?> redisCacheConfigurationClass, 
                                                   EnterpriseStarterProperties.Cache cacheConfig) throws Exception {
        // Cache method lookups to avoid repeated reflection calls
        java.lang.reflect.Method defaultCacheConfigMethod = redisCacheConfigurationClass.getMethod("defaultCacheConfig");
        java.lang.reflect.Method entryTtlMethod = redisCacheConfigurationClass.getMethod("entryTtl", Duration.class);
        
        Object defaultConfig = defaultCacheConfigMethod.invoke(null);
        return entryTtlMethod.invoke(defaultConfig, Duration.ofSeconds(cacheConfig.getRedisTtlSeconds()));
    }
    
    /**
     * Configures serialization for Redis cache (keys as strings, values as JSON).
     */
    private Object configureSerialization(Object defaultConfig, RedisClasses redisClasses) throws Exception {
        // Get SerializationPair class (cached in RedisClasses)
        Class<?> serializationPairClass = redisClasses.getSerializationPairClass();
        
        // Create serializers
        Object keySerializer = redisClasses.stringRedisSerializerClass.getConstructor().newInstance();
        Object valueSerializer = redisClasses.genericJackson2JsonRedisSerializerClass.getConstructor().newInstance();
        
        // Create serialization pairs
        java.lang.reflect.Method fromSerializerMethod = serializationPairClass.getMethod("fromSerializer", redisClasses.redisSerializerClass);
        Object keyPair = fromSerializerMethod.invoke(null, keySerializer);
        Object valuePair = fromSerializerMethod.invoke(null, valueSerializer);
        
        // Apply serialization configuration (cache method lookups)
        java.lang.reflect.Method serializeKeysWithMethod = redisClasses.redisCacheConfigurationClass.getMethod("serializeKeysWith", serializationPairClass);
        java.lang.reflect.Method serializeValuesWithMethod = redisClasses.redisCacheConfigurationClass.getMethod("serializeValuesWith", serializationPairClass);
        java.lang.reflect.Method disableCachingNullValuesMethod = redisClasses.redisCacheConfigurationClass.getMethod("disableCachingNullValues");
        
        defaultConfig = serializeKeysWithMethod.invoke(defaultConfig, keyPair);
        defaultConfig = serializeValuesWithMethod.invoke(defaultConfig, valuePair);
        defaultConfig = disableCachingNullValuesMethod.invoke(defaultConfig);
        
        return defaultConfig;
    }
    
    /**
     * Creates cache-specific configurations with different TTLs for specific caches.
     */
    private Map<String, Object> createCacheConfigurations(Object defaultConfig, 
                                                          Class<?> redisCacheConfigurationClass,
                                                          EnterpriseStarterProperties.Cache cacheConfig) throws Exception {
        // Pre-allocate HashMap with estimated capacity to avoid resizing
        int estimatedSize = Math.max(5, cacheConfig.getCacheNames().size() + 3);
        Map<String, Object> cacheConfigurations = new HashMap<>(estimatedSize);
        
        // Cache method lookup to avoid repeated reflection calls
        java.lang.reflect.Method entryTtlMethod = redisCacheConfigurationClass.getMethod("entryTtl", Duration.class);
        
        // Cache-specific TTLs
        Object tokensConfig = entryTtlMethod.invoke(defaultConfig, Duration.ofMinutes(15));
        Object configConfig = entryTtlMethod.invoke(defaultConfig, Duration.ofHours(24));
        
        cacheConfigurations.put("tokens", tokensConfig);
        cacheConfigurations.put("config", configConfig);
        cacheConfigurations.put("default", defaultConfig);
        
        // Add other cache names with default configuration
        for (String cacheName : cacheConfig.getCacheNames()) {
            cacheConfigurations.putIfAbsent(cacheName, defaultConfig);
        }
        
        return cacheConfigurations;
    }
    
    /**
     * Builds the RedisCacheManager using the builder pattern.
     */
    private CacheManager buildRedisCacheManager(RedisClasses redisClasses,
                                               Object redisConnectionFactory,
                                               Object defaultConfig,
                                               Map<String, Object> cacheConfigurations) throws Exception {
        // Create builder
        java.lang.reflect.Method builderMethod = redisClasses.redisCacheManagerClass.getMethod("builder", redisClasses.redisConnectionFactoryClass);
        Object builder = builderMethod.invoke(null, redisConnectionFactory);
        
        Class<?> builderClass = builder.getClass();
        
        // Find and call cacheDefaults method (handles different method names across versions)
        java.lang.reflect.Method cacheDefaultsMethod = findCacheDefaultsMethod(builderClass, redisClasses.redisCacheConfigurationClass);
        builder = cacheDefaultsMethod.invoke(builder, defaultConfig);
        
        // Configure initial cache configurations (cache method lookup)
        java.lang.reflect.Method withInitialCacheConfigurationsMethod = 
                builderClass.getMethod("withInitialCacheConfigurations", Map.class);
        builder = withInitialCacheConfigurationsMethod.invoke(builder, cacheConfigurations);
        
        // Enable transaction awareness (cache method lookup)
        java.lang.reflect.Method transactionAwareMethod = builderClass.getMethod("transactionAware");
        builder = transactionAwareMethod.invoke(builder);
        
        // Build and return (cache method lookup)
        java.lang.reflect.Method buildMethod = builderClass.getMethod("build");
        return (CacheManager) buildMethod.invoke(builder);
    }
    
    /**
     * Finds the cacheDefaults method on the builder, handling different method names across versions.
     */
    private java.lang.reflect.Method findCacheDefaultsMethod(Class<?> builderClass, 
                                                             Class<?> redisCacheConfigurationClass) 
            throws NoSuchMethodException {
        try {
            return builderClass.getMethod("cacheDefaults", redisCacheConfigurationClass);
        } catch (NoSuchMethodException e) {
            try {
                return builderClass.getMethod("withCacheDefaults", redisCacheConfigurationClass);
            } catch (NoSuchMethodException e2) {
                // Try to find any method that takes RedisCacheConfiguration
                for (java.lang.reflect.Method m : builderClass.getMethods()) {
                    if (m.getParameterCount() == 1 && 
                        m.getParameterTypes()[0].equals(redisCacheConfigurationClass)) {
                        return m;
                    }
                }
                throw new IllegalStateException("Could not find cacheDefaults method on RedisCacheManager.Builder");
            }
        }
    }

    /**
     * Multi-Level Cache Manager - Combines L1 and L2 with proper write-through.
     * 
     * Unlike CompositeCacheManager, this implementation:
     * - Reads from L1 first, then L2, then source
     * - Writes to BOTH L1 and L2 on cache miss (write-through)
     * - Evicts from BOTH L1 and L2 on eviction
     * 
     * This ensures Redis (L2) is properly populated and can be shared across instances.
     * This is the primary cache manager when Redis is available.
     */
    @Bean
    @Primary
    @ConditionalOnBean(name = "redisCacheManager")
    public CacheManager compositeCacheManager(CacheManager caffeineCacheManager,
                                              CacheManager redisCacheManager) {
        logger.info("Creating MultiLevelCacheManager with L1 (Caffeine) and L2 (Redis)");
        MultiLevelCacheManager multiLevelCacheManager = new MultiLevelCacheManager(caffeineCacheManager, redisCacheManager);
        logger.info("MultiLevelCacheManager created with {} cache(s)", multiLevelCacheManager.getCacheNames().size());
        return multiLevelCacheManager;
    }
}

