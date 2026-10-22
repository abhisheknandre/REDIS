package com.example.redis_product_catalog.config;

import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.LoggingCacheErrorHandler;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheErrorConfig implements CachingConfigurer {

    @Override
    public CacheErrorHandler errorHandler() {
        // logs a one-line warning and swallows the error (false = no stack traces, avoids log floods)
        return new LoggingCacheErrorHandler(false);
    }
}