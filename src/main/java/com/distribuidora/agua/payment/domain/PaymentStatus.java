package com.distribuidora.agua.payment.domain;

/**
 * Status of a payment transaction.
 */
public enum PaymentStatus {
    PENDING,
    PAID,
    FAILED,
    REFUNDED
}
