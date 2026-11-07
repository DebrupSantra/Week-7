package com.example.ecommerce.dto;

import com.example.ecommerce.entity.CartItem;

import java.math.BigDecimal;

public record CartLineResponse(
        Long id,
        Long productId,
        String sku,
        String name,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal
) {

    public static CartLineResponse from(CartItem item) {
        BigDecimal lineTotal = item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        return new CartLineResponse(item.getId(), item.getProduct().getId(), item.getProduct().getSku(),
                item.getProduct().getName(), item.getProduct().getPrice(), item.getQuantity(), lineTotal);
    }
}
