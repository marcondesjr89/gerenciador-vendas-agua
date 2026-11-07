package com.distribuidora.agua.payment.application;

import com.distribuidora.agua.orders.domain.PaymentMethod;

import java.math.BigDecimal;

/**
 * Command representing input to process a payment.
 */
public record ProcessPaymentCommand(
        Long orderId,
        PaymentMethod paymentMethod,
        BigDecimal paidAmount
) {
}
