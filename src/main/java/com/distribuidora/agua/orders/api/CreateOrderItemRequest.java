package com.distribuidora.agua.orders.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Request DTO for an item within an order creation payload.
 */
public record CreateOrderItemRequest(
        @NotNull(message = "O ID do produto e obrigatorio")
        Long productId,

        @NotNull(message = "A quantidade do item e obrigatoria")
        @Positive(message = "A quantidade deve ser maior que zero")
        Integer quantity,

        Boolean bottlesReturned
) {
    public boolean isBottlesReturned() {
        return bottlesReturned != null && bottlesReturned;
    }
}
