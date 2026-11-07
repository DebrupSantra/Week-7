package com.example.ecommerce.dto;

import com.example.ecommerce.entity.ShoppingCart;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(List<CartLineResponse> items, BigDecimal total) {

    public static CartResponse from(ShoppingCart cart) {
        List<CartLineResponse> lines = cart.getItems().stream().map(CartLineResponse::from).toList();
        BigDecimal total = lines.stream().map(CartLineResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(lines, total);
    }
}
