package com.distribuidora.agua.orders.api;

import com.distribuidora.agua.orders.domain.OrderItem;

import java.math.BigDecimal;

/**
 * Response DTO for an order line item.
 */
public record OrderItemResponse(
        Long id,
        Long productId,
        String productSku,
        String productName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal totalItemAmount,
        boolean bottlesReturned
) {
    public static OrderItemResponse fromDomain(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getSku(),
                item.getProduct().getName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getTotalItemAmount(),
                item.isBottlesReturned()
        );
    }
}
