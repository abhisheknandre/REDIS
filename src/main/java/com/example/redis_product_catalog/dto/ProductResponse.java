package com.example.redis_product_catalog.dto;

import com.example.redis_product_catalog.entity.Product;

import java.math.BigDecimal;

public class ProductResponse {

    private Long id;
    private String name;
    private BigDecimal price;
    private boolean active;

    /** Required by Jackson when reading cached JSON back from Redis. */
    public ProductResponse() {
    }

    public ProductResponse(Long id, String name, BigDecimal price, boolean active) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.active = active;
    }

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.isActive());
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
    public boolean isActive() { return active; }

    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public void setActive(boolean active) { this.active = active; }
}