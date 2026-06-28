package com.notificationengine.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis configuration providing Lettuce connection factory,
 * RedisTemplate and a granular per-cache TTL configuration.
 */
@Slf4j
@Configuration
public class RedisConfig implements CachingConfigurer {

    @Value("${spring.cache.redis.time-to-live:600000}")
    private long defaultTtlMs;

    // -----------------------------------------------------------------------
    // RedisTemplate
    // -----------------------------------------------------------------------

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer keySerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer();

        template.setKeySerializer(keySerializer);
        template.setHashKeySerializer(keySerializer);
        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);
        template.afterPropertiesSet();

        log.info("RedisTemplate configured with JSON serialization");
        return template;
    }

    // -----------------------------------------------------------------------
    // Cache Manager
    // -----------------------------------------------------------------------

    @Bean
    @Override
    public CacheManager cacheManager() {
        RedisCacheConfiguration defaultConfig = defaultCacheConfig();

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        // Templates cache — longer TTL since they change infrequently
        cacheConfigurations.put("templates", defaultCacheConfig(Duration.ofHours(2)));
        // Preferences cache
        cacheConfigurations.put("preferences", defaultCacheConfig(Duration.ofMinutes(30)));
        // Channels cache
        cacheConfigurations.put("channels", defaultCacheConfig(Duration.ofHours(1)));
        // Analytics cache — short TTL for near real-time data
        cacheConfigurations.put("analytics", defaultCacheConfig(Duration.ofMinutes(5)));

        return RedisCacheManager.builder(
                        org.springframework.data.redis.cache.RedisCacheWriter
                                .nonLockingRedisCacheWriter(redisConnectionFactory()))
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    private RedisCacheConfiguration defaultCacheConfig() {
        return defaultCacheConfig(Duration.ofMillis(defaultTtlMs));
    }

    private RedisCacheConfiguration defaultCacheConfig(Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .disableCachingNullValues()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new GenericJackson2JsonRedisSerializer()));
    }

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory();
    }
}
