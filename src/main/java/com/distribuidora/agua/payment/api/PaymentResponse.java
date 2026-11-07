package com.distribuidora.agua.payment.api;

import com.distribuidora.agua.orders.domain.PaymentMethod;
import com.distribuidora.agua.payment.domain.Payment;
import com.distribuidora.agua.payment.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Public response DTO for payment receipts.
 */
public record PaymentResponse(
        Long id,
        Long orderId,
        String orderCode,
        String idempotencyKey,
        PaymentMethod paymentMethod,
        BigDecimal paidAmount,
        PaymentStatus status,
        Instant processedAt
) {
    public static PaymentResponse fromDomain(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrder().getId(),
                payment.getOrder().getOrderCode(),
                payment.getIdempotencyKey(),
                payment.getPaymentMethod(),
                payment.getPaidAmount(),
                payment.getStatus(),
                payment.getProcessedAt()
        );
    }
}
