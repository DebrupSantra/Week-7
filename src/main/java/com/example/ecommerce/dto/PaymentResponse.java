package com.example.ecommerce.dto;

import com.example.ecommerce.entity.Payment;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        Long id,
        Long orderId,
        String method,
        String status,
        BigDecimal amount,
        String providerReference,
        Instant createdAt
) {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(payment.getId(), payment.getOrder().getId(),
                payment.getMethod().name(), payment.getStatus().name(), payment.getAmount(),
                payment.getProviderReference(), payment.getCreatedAt());
    }
}
