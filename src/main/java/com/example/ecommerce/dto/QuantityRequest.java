package com.example.ecommerce.dto;

import jakarta.validation.constraints.Positive;

public record QuantityRequest(@Positive int quantity) {
}
