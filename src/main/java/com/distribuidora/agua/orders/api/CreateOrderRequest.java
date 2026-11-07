package com.distribuidora.agua.orders.api;

import com.distribuidora.agua.orders.application.CreateOrderCommand;
import com.distribuidora.agua.orders.application.OrderItemCommand;
import com.distribuidora.agua.orders.domain.Modality;
import com.distribuidora.agua.orders.domain.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Request payload for creating a new sales order.
 */
public record CreateOrderRequest(
        @NotBlank(message = "O nome do cliente e obrigatorio")
        String customerName,

        @NotBlank(message = "O telefone do cliente e obrigatorio")
        String customerPhone,

        @NotNull(message = "A modalidade do pedido (PICKUP / DELIVERY) e obrigatoria")
        Modality modality,

        @NotNull(message = "A forma de pagamento e obrigatoria")
        PaymentMethod paymentMethod,

        @NotEmpty(message = "O pedido deve conter pelo menos um item")
        List<@Valid CreateOrderItemRequest> items
) {
    public CreateOrderCommand toCommand() {
        List<OrderItemCommand> itemCommands = items.stream()
                .map(item -> new OrderItemCommand(item.productId(), item.quantity(), item.isBottlesReturned()))
                .toList();

        return new CreateOrderCommand(
                customerName,
                customerPhone,
                modality,
                paymentMethod,
                itemCommands
        );
    }
}
