package com.example.ecommerce.dto;

import com.example.ecommerce.entity.OrderItem;

import java.math.BigDecimal;

public record OrderItemResponse(
        String sku,
        String productName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {

    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(item.getProductSku(), item.getProductName(), item.getQuantity(),
                item.getUnitPrice(), item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
    }
}
