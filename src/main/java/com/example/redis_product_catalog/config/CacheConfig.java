package com.example.redis_product_catalog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.data.redis.cache.BatchStrategies;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching(order = Ordered.LOWEST_PRECEDENCE - 1)   // cache advice OUTSIDE @Transactional
public class CacheConfig {

    public static final String PRODUCT_CACHE = "product";

    @Bean
    public RedisCacheManager cacheManager(
            RedisConnectionFactory factory,
            RedisSerializer<Object> jsonRedisSerializer,
            @Value("${spring.cache.redis.time-to-live:10m}") String defaultTtl,
            @Value("${spring.cache.redis.key-prefix:catalog::}") String keyPrefix,
            @Value("${app.cache.ttl.product:30m}") String productTtl) {

        // SCAN-based clearing so cache.clear() never blocks Redis with KEYS
        RedisCacheWriter writer = RedisCacheWriter.nonLockingRedisCacheWriter(
                factory, BatchStrategies.scan(1000));

        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(SerializationPair.fromSerializer(jsonRedisSerializer))
                .prefixCacheNameWith(keyPrefix)          // keys look like catalog::product::42
                .disableCachingNullValues()
                .entryTtl(DurationStyle.detectAndParse(defaultTtl));

        return RedisCacheManager.builder(writer)
                .cacheDefaults(defaults)
                .withCacheConfiguration(PRODUCT_CACHE,
                        defaults.entryTtl(DurationStyle.detectAndParse(productTtl)))
                .enableStatistics()                      // <-- the new line
                .build();
    }
}