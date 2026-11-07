package com.distribuidora.agua.orders.application;

import com.distribuidora.agua.orders.domain.Modality;
import com.distribuidora.agua.orders.domain.PaymentMethod;

import java.util.List;

/**
 * Command representing input for creating a new sales order.
 */
public record CreateOrderCommand(
        String customerName,
        String customerPhone,
        Modality modality,
        PaymentMethod paymentMethod,
        List<OrderItemCommand> items
) {
}
