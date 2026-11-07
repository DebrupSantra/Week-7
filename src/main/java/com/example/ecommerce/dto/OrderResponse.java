package com.example.ecommerce.dto;

import com.example.ecommerce.entity.PurchaseOrder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        String status,
        BigDecimal total,
        Instant createdAt,
        List<OrderItemResponse> items
) {

    public static OrderResponse from(PurchaseOrder order) {
        return new OrderResponse(order.getId(), order.getStatus().name(), order.getTotal(),
                order.getCreatedAt(), order.getItems().stream().map(OrderItemResponse::from).toList());
    }
}
