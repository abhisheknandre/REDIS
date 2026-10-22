package com.example.redis_product_catalog.dto;

import com.example.redis_product_catalog.entity.Product;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductRequest(

        @NotBlank(message = "name must not be blank")
        @Size(max = 150, message = "name must be at most 150 characters")
        String name,

        @NotNull(message = "price is required")
        @Positive(message = "price must be greater than zero")
        @Digits(integer = 10, fraction = 2, message = "price must have at most 10 integer digits and 2 decimals")
        BigDecimal price
) {

    /** Maps this request to a new entity (active defaults to true). */
    public Product toEntity() {
        return new Product(name.trim(), price);
    }
}