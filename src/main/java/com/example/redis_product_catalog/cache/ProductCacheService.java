package com.example.redis_product_catalog.cache;

import com.example.redis_product_catalog.dto.ProductResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static com.example.redis_product_catalog.config.CacheConfig.PRODUCT_CACHE;

@Service
public class ProductCacheService {

    private static final Logger log = LoggerFactory.getLogger(ProductCacheService.class);

    private final CacheManager cacheManager;

    public ProductCacheService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public Optional<ProductResponse> get(Long id) {
        try {
            Cache cache = cacheManager.getCache(PRODUCT_CACHE);
            return cache == null ? Optional.empty()
                    : Optional.ofNullable(cache.get(id, ProductResponse.class));
        } catch (RuntimeException ex) {
            // Redis down, or a stale/incompatible payload: treat as a miss and drop the bad entry
            log.warn("Cache read failed for product {}: {}", id, ex.getMessage());
            evict(id);
            return Optional.empty();
        }
    }

    public void put(ProductResponse product) {
        try {
            Cache cache = cacheManager.getCache(PRODUCT_CACHE);
            if (cache != null) {
                cache.put(product.getId(), product);
            }
        } catch (RuntimeException ex) {
            log.warn("Cache write failed for product {}: {}", product.getId(), ex.getMessage());
        }
    }

    public void evict(Long id) {
        try {
            Cache cache = cacheManager.getCache(PRODUCT_CACHE);
            if (cache != null) {
                cache.evict(id);
            }
        } catch (RuntimeException ex) {
            log.warn("Cache evict failed for product {}: {}", id, ex.getMessage());
        }
    }

    /** Clears only this cache's keys (catalog::product::*), using SCAN rather than KEYS. */
    public void evictAll() {
        try {
            Cache cache = cacheManager.getCache(PRODUCT_CACHE);
            if (cache != null) {
                cache.clear();
            }
        } catch (RuntimeException ex) {
            log.warn("Cache clear failed: {}", ex.getMessage());
        }
    }
}