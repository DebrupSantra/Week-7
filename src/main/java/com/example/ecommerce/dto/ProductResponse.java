package com.example.ecommerce.dto;

import com.example.ecommerce.entity.Product;

import java.math.BigDecimal;
import java.util.Set;

public record ProductResponse(
        Long id,
        String sku,
        String name,
        String description,
        BigDecimal price,
        int inventoryQuantity,
        boolean active,
        Set<CategoryResponse> categories
) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(), product.getSku(), product.getName(), product.getDescription(),
                product.getPrice(), product.getInventoryQuantity(), product.isActive(),
                product.getCategories().stream().map(CategoryResponse::from)
                        .collect(java.util.stream.Collectors.toUnmodifiableSet()));
    }
}
