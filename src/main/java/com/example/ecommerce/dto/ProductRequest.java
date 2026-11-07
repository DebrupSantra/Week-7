package com.example.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Digits;

import java.math.BigDecimal;
import java.util.Set;

public record ProductRequest(
        @NotBlank @Size(max = 80) String sku,
        @NotBlank @Size(max = 200) String name,
        @Size(max = 4000) String description,
        @NotNull @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal price,
        @PositiveOrZero int inventoryQuantity,
        boolean active,
        @NotNull Set<@NotNull @Positive Long> categoryIds
) {
}
