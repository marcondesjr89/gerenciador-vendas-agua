package com.distribuidora.agua.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String sku;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "capacity_liters", nullable = false, precision = 5, scale = 2)
    private BigDecimal capacityLiters;

    @Column(nullable = false)
    private boolean returnable;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false, length = 30)
    private ProductType productType;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "stock_full", nullable = false)
    private int stockFull;

    @Column(name = "stock_empty", nullable = false)
    private int stockEmpty;

    @Column(nullable = false)
    private boolean active = true;

    @Version
    @Column(nullable = false)
    private Long version;

    protected Product() {
    }

    public Product(Long id, String sku, String name, BigDecimal capacityLiters, boolean returnable,
                   ProductType productType, BigDecimal unitPrice, int stockFull, int stockEmpty,
                   boolean active, Long version) {
        this.id = id;
        this.sku = Objects.requireNonNull(sku, "SKU must not be null");
        this.name = Objects.requireNonNull(name, "Name must not be null");
        this.capacityLiters = Objects.requireNonNull(capacityLiters, "Capacity in liters must not be null");
        if (capacityLiters.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Capacity in liters must be greater than zero");
        }
        this.returnable = returnable;
        this.productType = Objects.requireNonNull(productType, "Product type must not be null");
        this.unitPrice = Objects.requireNonNull(unitPrice, "Unit price must not be null");
        if (unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Unit price must be greater than zero");
        }
        if (stockFull < 0 || stockEmpty < 0) {
            throw new IllegalArgumentException("Stock balances cannot be negative");
        }
        this.stockFull = stockFull;
        this.stockEmpty = stockEmpty;
        this.active = active;
        this.version = version;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getCapacityLiters() {
        return capacityLiters;
    }

    public boolean isReturnable() {
        return returnable;
    }

    public ProductType getProductType() {
        return productType;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public int getStockFull() {
        return stockFull;
    }

    public int getStockEmpty() {
        return stockEmpty;
    }

    public boolean isActive() {
        return active;
    }

    public Long getVersion() {
        return version;
    }

    public void setStockFull(int stockFull) {
        if (stockFull < 0) {
            throw new IllegalArgumentException("Stock full cannot be negative");
        }
        this.stockFull = stockFull;
    }

    public void setStockEmpty(int stockEmpty) {
        if (stockEmpty < 0) {
            throw new IllegalArgumentException("Stock empty cannot be negative");
        }
        this.stockEmpty = stockEmpty;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public static class Builder {
        private Long id;
        private String sku;
        private String name;
        private BigDecimal capacityLiters;
        private boolean returnable = true;
        private ProductType productType;
        private BigDecimal unitPrice;
        private int stockFull = 0;
        private int stockEmpty = 0;
        private boolean active = true;
        private Long version;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder sku(String sku) {
            this.sku = sku;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder capacityLiters(BigDecimal capacityLiters) {
            this.capacityLiters = capacityLiters;
            return this;
        }

        public Builder returnable(boolean returnable) {
            this.returnable = returnable;
            return this;
        }

        public Builder productType(ProductType productType) {
            this.productType = productType;
            return this;
        }

        public Builder unitPrice(BigDecimal unitPrice) {
            this.unitPrice = unitPrice;
            return this;
        }

        public Builder stockFull(int stockFull) {
            this.stockFull = stockFull;
            return this;
        }

        public Builder stockEmpty(int stockEmpty) {
            this.stockEmpty = stockEmpty;
            return this;
        }

        public Builder active(boolean active) {
            this.active = active;
            return this;
        }

        public Builder version(Long version) {
            this.version = version;
            return this;
        }

        public Product build() {
            return new Product(id, sku, name, capacityLiters, returnable, productType,
                    unitPrice, stockFull, stockEmpty, active, version);
        }
    }
}
