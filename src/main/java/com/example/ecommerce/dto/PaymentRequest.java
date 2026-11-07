package com.example.ecommerce.dto;

import com.example.ecommerce.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PaymentRequest(
        @NotNull PaymentMethod method,
        @NotBlank @Size(min = 16, max = 100)
        @Pattern(regexp = "[A-Za-z0-9._:-]{16,100}") String idempotencyKey
) {
}
