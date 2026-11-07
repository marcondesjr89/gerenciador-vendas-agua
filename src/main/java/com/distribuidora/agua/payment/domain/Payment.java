package com.distribuidora.agua.payment.domain;

import com.distribuidora.agua.orders.domain.Order;
import com.distribuidora.agua.orders.domain.PaymentMethod;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Root Aggregate representing an idempotent payment execution for an order.
 */
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 80)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Column(name = "paid_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal paidAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "processed_at", nullable = false, updatable = false)
    private Instant processedAt;

    protected Payment() {
    }

    public Payment(Order order, String idempotencyKey, PaymentMethod paymentMethod, BigDecimal paidAmount, PaymentStatus status) {
        this(null, order, idempotencyKey, paymentMethod, paidAmount, status, Instant.now());
    }

    public Payment(Long id, Order order, String idempotencyKey, PaymentMethod paymentMethod, BigDecimal paidAmount, PaymentStatus status, Instant processedAt) {
        this.id = id;
        this.order = Objects.requireNonNull(order, "Order cannot be null");
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "Idempotency key cannot be null");
        this.paymentMethod = Objects.requireNonNull(paymentMethod, "Payment method cannot be null");
        this.paidAmount = Objects.requireNonNull(paidAmount, "Paid amount cannot be null");
        this.status = Objects.requireNonNull(status, "Payment status cannot be null");
        this.processedAt = processedAt != null ? processedAt : Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
