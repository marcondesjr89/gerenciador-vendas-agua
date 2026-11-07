package com.distribuidora.agua.orders.application;

/**
 * Command input for an item within an order creation request.
 */
public record OrderItemCommand(
        Long productId,
        int quantity,
        boolean bottlesReturned
) {
}
