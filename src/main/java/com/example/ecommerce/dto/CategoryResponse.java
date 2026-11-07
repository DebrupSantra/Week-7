package com.example.ecommerce.dto;

import com.example.ecommerce.entity.Category;

public record CategoryResponse(Long id, String name, String description, Long parentId) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getParent() == null ? null : category.getParent().getId());
    }
}
