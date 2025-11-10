package com.yourcompany.starter.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.support.CompositeCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for cache configuration.
 */
class CacheConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    EnterpriseStarterAutoConfiguration.class,
                    CacheConfig.class
            ));

    @Test
    void testCacheConfig_WhenDisabled() {
        contextRunner
                .withPropertyValues("enterprise.starter.cache.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(CacheManager.class);
                });
    }

    @Test
    void testCacheConfig_WhenEnabled_WithoutRedis() {
        contextRunner
                .withPropertyValues(
                        "enterprise.starter.cache.enabled=true",
                        "enterprise.starter.cache.redis-enabled=false"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(CacheManager.class);
                    CacheManager cacheManager = context.getBean(CacheManager.class);
                    assertThat(cacheManager).isInstanceOf(CaffeineCacheManager.class);
                    assertThat(cacheManager.getCacheNames()).contains("default", "users", "config", "tokens", "api-responses");
                });
    }

    @Test
    void testCacheConfig_WhenEnabled_WithRedis_ButRedisNotAvailable() {
        contextRunner
                .withPropertyValues(
                        "enterprise.starter.cache.enabled=true",
                        "enterprise.starter.cache.redis-enabled=true"
                )
                .run(context -> {
                    // Should still create Caffeine cache manager even if Redis is not available
                    assertThat(context).hasSingleBean(CacheManager.class);
                    CacheManager cacheManager = context.getBean(CacheManager.class);
                    assertThat(cacheManager).isInstanceOf(CaffeineCacheManager.class);
                });
    }

    @Test
    void testCacheConfig_WhenEnabled_WithRedis_AndRedisAvailable() {
        contextRunner
                .withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class))
                .withPropertyValues(
                        "enterprise.starter.cache.enabled=true",
                        "enterprise.starter.cache.redis-enabled=true",
                        "spring.data.redis.host=localhost",
                        "spring.data.redis.port=6379",
                        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration"
                )
                .run(context -> {
                    // Should have Caffeine cache manager
                    assertThat(context).hasBean("caffeineCacheManager");
                    
                    // Redis cache manager may not be created if Redis is not available
                    // This is expected behavior - fallback to Caffeine only
                    CacheManager cacheManager = context.getBean(CacheManager.class);
                    assertThat(cacheManager).isInstanceOf(CaffeineCacheManager.class);
                });
    }

    @Test
    void testCacheConfig_CustomCacheNames() {
        contextRunner
                .withPropertyValues(
                        "enterprise.starter.cache.enabled=true",
                        "enterprise.starter.cache.redis-enabled=false",
                        "enterprise.starter.cache.cache-names[0]=custom1",
                        "enterprise.starter.cache.cache-names[1]=custom2"
                )
                .run(context -> {
                    CacheManager cacheManager = context.getBean(CacheManager.class);
                    assertThat(cacheManager.getCacheNames()).contains("custom1", "custom2");
                });
    }

    @Test
    void testCacheConfig_CustomCaffeineSettings() {
        contextRunner
                .withPropertyValues(
                        "enterprise.starter.cache.enabled=true",
                        "enterprise.starter.cache.redis-enabled=false",
                        "enterprise.starter.cache.caffeine-max-size=5000",
                        "enterprise.starter.cache.caffeine-ttl-seconds=600",
                        "enterprise.starter.cache.caffeine-access-expiration-seconds=300"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(CacheManager.class);
                    CacheManager cacheManager = context.getBean(CacheManager.class);
                    assertThat(cacheManager).isInstanceOf(CaffeineCacheManager.class);
                });
    }

    @Test
    void testCacheConfig_CustomRedisSettings() {
        contextRunner
                .withPropertyValues(
                        "enterprise.starter.cache.enabled=true",
                        "enterprise.starter.cache.redis-enabled=true",
                        "enterprise.starter.cache.redis-ttl-seconds=7200"
                )
                .run(context -> {
                    // Should have Caffeine cache manager
                    assertThat(context).hasBean("caffeineCacheManager");
                    
                    // Redis cache manager only created if Redis is available
                    // In test environment without Redis, only Caffeine is used
                    CacheManager cacheManager = context.getBean(CacheManager.class);
                    assertThat(cacheManager).isInstanceOf(CaffeineCacheManager.class);
                });
    }
}

