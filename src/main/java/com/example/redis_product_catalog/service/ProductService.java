package com.example.redis_product_catalog.service;

import com.example.redis_product_catalog.dto.ProductRequest;
import com.example.redis_product_catalog.dto.ProductResponse;
import com.example.redis_product_catalog.entity.Product;
import com.example.redis_product_catalog.exception.ProductNotFoundException;
import com.example.redis_product_catalog.repository.ProductRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.example.redis_product_catalog.config.CacheConfig.PRODUCT_CACHE;

@Service
public class ProductService {

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    /**
     * No @Transactional here on purpose: a cache hit must not open a transaction or borrow a DB connection.
     * The repository call runs in its own short read-only transaction on a miss.
     * sync = true makes concurrent misses for the same id load from the DB once (per JVM instance).
     * Only active products are cached; unknown/inactive ids throw 404 and are never cached.
     */
    @Cacheable(cacheNames = PRODUCT_CACHE, key = "#id", sync = true)
    public ProductResponse getById(Long id) {
        return repository.findByIdAndActiveTrue(id)
                .map(ProductResponse::from)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    /** Pages are not cached: they are volatile and serialize poorly. */
    @Transactional(readOnly = true)
    public Page<ProductResponse> getAll(String name, Pageable pageable) {
        Page<Product> page = (name == null || name.isBlank())
                ? repository.findByActiveTrue(pageable)
                : repository.findByActiveTrueAndNameContainingIgnoreCase(name.trim(), pageable);
        return page.map(ProductResponse::from);
    }

    /** Write-through: the new product is cached right after the transaction commits. */
    @Transactional
    @CachePut(cacheNames = PRODUCT_CACHE, key = "#result.id")
    public ProductResponse create(ProductRequest request) {
        return ProductResponse.from(repository.save(request.toEntity()));
    }

    /** Refreshes the cached entry with the committed state. Inactive/unknown ids throw 404. */
    @Transactional
    @CachePut(cacheNames = PRODUCT_CACHE, key = "#id")
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = repository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        product.update(request.name().trim(), request.price());   // dirty checking flushes on commit
        return ProductResponse.from(product);
    }

    /** Soft delete. The entry is evicted only if the method succeeds. */
    @Transactional
    @CacheEvict(cacheNames = PRODUCT_CACHE, key = "#id")
    public void deactivate(Long id) {
        Product product = repository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        product.deactivate();
    }
}