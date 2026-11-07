package com.distribuidora.agua.orders.domain;

import com.distribuidora.agua.catalog.domain.Product;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Root Aggregate representing a mineral water sales order.
 */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_code", nullable = false, unique = true, length = 40)
    private String orderCode;

    @Column(name = "customer_name", nullable = false, length = 100)
    private String customerName;

    @Column(name = "customer_phone", nullable = false, length = 20)
    private String customerPhone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Modality modality;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "delivery_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal deliveryFee;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();

    protected Order() {
    }

    private Order(Builder builder) {
        this.id = builder.id;
        this.orderCode = Objects.requireNonNull(builder.orderCode, "Order code cannot be null");
        this.customerName = Objects.requireNonNull(builder.customerName, "Customer name cannot be null");
        this.customerPhone = Objects.requireNonNull(builder.customerPhone, "Customer phone cannot be null");
        this.modality = Objects.requireNonNull(builder.modality, "Modality cannot be null");
        this.paymentMethod = Objects.requireNonNull(builder.paymentMethod, "Payment method cannot be null");
        this.status = builder.status != null ? builder.status : OrderStatus.PENDING;
        this.deliveryFee = builder.deliveryFee != null ? builder.deliveryFee : BigDecimal.ZERO;
        this.subtotal = builder.subtotal != null ? builder.subtotal : BigDecimal.ZERO;
        this.totalAmount = builder.totalAmount != null ? builder.totalAmount : this.subtotal.add(this.deliveryFee);
        this.createdAt = builder.createdAt != null ? builder.createdAt : Instant.now();
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Adds an item to the order and links bidirectional relationship.
     */
    public void addItem(Product product, int quantity, boolean bottlesReturned) {
        OrderItem item = new OrderItem(this, product, quantity, bottlesReturned);
        this.items.add(item);
    }

    /**
     * Recalculates subtotal and total amount based on items and delivery fee.
     */
    public void recalculateTotals() {
        this.subtotal = items.stream()
                .map(OrderItem::getTotalItemAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.totalAmount = this.subtotal.add(this.deliveryFee != null ? this.deliveryFee : BigDecimal.ZERO);
    }

    /**
     * Controls valid transitions across the order state machine.
     */
    public void transitionTo(OrderStatus nextStatus) {
        Objects.requireNonNull(nextStatus, "Next status cannot be null");

        if (this.status.isTerminal()) {
            throw new IllegalStateException(String.format(
                    "Cannot transition order [%s] from terminal status [%s] to [%s]",
                    orderCode, this.status, nextStatus
            ));
        }

        if (nextStatus == OrderStatus.CANCELLED) {
            this.status = OrderStatus.CANCELLED;
            return;
        }

        switch (this.status) {
            case PENDING -> {
                if (nextStatus == OrderStatus.CONFIRMED) {
                    this.status = OrderStatus.CONFIRMED;
                } else {
                    throw new IllegalStateException(String.format(
                            "Invalid transition from PENDING to [%s]. Orders must be CONFIRMED first.", nextStatus
                    ));
                }
            }
            case CONFIRMED -> {
                if (nextStatus == OrderStatus.IN_TRANSIT) {
                    if (this.modality == Modality.PICKUP) {
                        throw new IllegalStateException("Orders with modality PICKUP cannot transition to IN_TRANSIT");
                    }
                    this.status = OrderStatus.IN_TRANSIT;
                } else if (nextStatus == OrderStatus.DELIVERED) {
                    this.status = OrderStatus.DELIVERED;
                } else {
                    throw new IllegalStateException(String.format(
                            "Invalid transition from CONFIRMED to [%s]", nextStatus
                    ));
                }
            }
            case IN_TRANSIT -> {
                if (nextStatus == OrderStatus.DELIVERED) {
                    this.status = OrderStatus.DELIVERED;
                } else {
                    throw new IllegalStateException(String.format(
                            "Invalid transition from IN_TRANSIT to [%s]. Only DELIVERED or CANCELLED allowed.", nextStatus
                    ));
                }
            }
            default -> throw new IllegalStateException(String.format(
                    "Unexpected current status: [%s]", this.status
            ));
        }
    }

    public void cancel() {
        transitionTo(OrderStatus.CANCELLED);
    }

    public Long getId() {
        return id;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public Modality getModality() {
        return modality;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getDeliveryFee() {
        return deliveryFee;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public static class Builder {
        private Long id;
        private String orderCode;
        private String customerName;
        private String customerPhone;
        private Modality modality;
        private OrderStatus status = OrderStatus.PENDING;
        private PaymentMethod paymentMethod;
        private BigDecimal subtotal = BigDecimal.ZERO;
        private BigDecimal deliveryFee = BigDecimal.ZERO;
        private BigDecimal totalAmount;
        private Instant createdAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder orderCode(String orderCode) {
            this.orderCode = orderCode;
            return this;
        }

        public Builder customerName(String customerName) {
            this.customerName = customerName;
            return this;
        }

        public Builder customerPhone(String customerPhone) {
            this.customerPhone = customerPhone;
            return this;
        }

        public Builder modality(Modality modality) {
            this.modality = modality;
            return this;
        }

        public Builder status(OrderStatus status) {
            this.status = status;
            return this;
        }

        public Builder paymentMethod(PaymentMethod paymentMethod) {
            this.paymentMethod = paymentMethod;
            return this;
        }

        public Builder subtotal(BigDecimal subtotal) {
            this.subtotal = subtotal;
            return this;
        }

        public Builder deliveryFee(BigDecimal deliveryFee) {
            this.deliveryFee = deliveryFee;
            return this;
        }

        public Builder totalAmount(BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Order build() {
            return new Order(this);
        }
    }
}
