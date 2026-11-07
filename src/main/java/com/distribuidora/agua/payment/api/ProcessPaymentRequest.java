package com.distribuidora.agua.payment.api;

import com.distribuidora.agua.orders.domain.PaymentMethod;
import com.distribuidora.agua.payment.application.ProcessPaymentCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Request payload for processing an idempotent payment.
 */
public record ProcessPaymentRequest(
        @NotNull(message = "O ID do pedido e obrigatorio")
        Long orderId,

        @NotNull(message = "A forma de pagamento e obrigatoria")
        PaymentMethod paymentMethod,

        @NotNull(message = "O valor pago e obrigatorio")
        @Positive(message = "O valor pago deve ser maior que zero")
        BigDecimal paidAmount
) {
    public ProcessPaymentCommand toCommand() {
        return new ProcessPaymentCommand(orderId, paymentMethod, paidAmount);
    }
}
