package com.distribuidora.agua.orders.api;

import com.distribuidora.agua.orders.domain.OrderStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for updating the status of an order.
 */
public record UpdateOrderStatusRequest(
        @NotNull(message = "O novo status e obrigatorio")
        OrderStatus status,

        String cancellationReason
) {
}
