package com.distribuidora.agua.orders.domain;

import com.distribuidora.agua.catalog.domain.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Line item belonging to an Order.
 */
@Entity
@Table(
        name = "order_items",
        indexes = {
                @Index(name = "idx_order_items_order_id", columnList = "order_id")
        }
)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_item_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalItemAmount;

    @Column(name = "bottles_returned", nullable = false)
    private boolean bottlesReturned;

    protected OrderItem() {
    }

    public OrderItem(Order order, Product product, int quantity, boolean bottlesReturned) {
        this.order = Objects.requireNonNull(order, "Order cannot be null");
        this.product = Objects.requireNonNull(product, "Product cannot be null");
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
        this.quantity = quantity;
        this.unitPrice = product.getUnitPrice();
        this.totalItemAmount = this.unitPrice.multiply(BigDecimal.valueOf(quantity));
        this.bottlesReturned = bottlesReturned;
    }

    public Long getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Product getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getTotalItemAmount() {
        return totalItemAmount;
    }

    public boolean isBottlesReturned() {
        return bottlesReturned;
    }

    public void setBottlesReturned(boolean bottlesReturned) {
        this.bottlesReturned = bottlesReturned;
    }
}
