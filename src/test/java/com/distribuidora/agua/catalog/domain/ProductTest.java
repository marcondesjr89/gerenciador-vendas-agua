package com.distribuidora.agua.catalog.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    @Test
    @DisplayName("Should create valid product with all required fields")
    void shouldCreateValidProduct() {
        Product product = Product.builder()
                .sku("GALLON-20L")
                .name("Mineral Water Gallon 20L")
                .capacityLiters(new BigDecimal("20.00"))
                .returnable(true)
                .productType(ProductType.FULL_WATER)
                .unitPrice(new BigDecimal("30.00"))
                .stockFull(50)
                .stockEmpty(20)
                .active(true)
                .build();

        assertThat(product.getSku()).isEqualTo("GALLON-20L");
        assertThat(product.getName()).isEqualTo("Mineral Water Gallon 20L");
        assertThat(product.getCapacityLiters()).isEqualByComparingTo("20.00");
        assertThat(product.isReturnable()).isTrue();
        assertThat(product.getProductType()).isEqualTo(ProductType.FULL_WATER);
        assertThat(product.getUnitPrice()).isEqualByComparingTo("30.00");
        assertThat(product.getStockFull()).isEqualTo(50);
        assertThat(product.getStockEmpty()).isEqualTo(20);
        assertThat(product.isActive()).isTrue();
    }

    @Test
    @DisplayName("Should reject product creation with required null fields")
    void shouldRejectNullFields() {
        Product.Builder baseBuilder = Product.builder()
                .sku("TEST-SKU")
                .name("Test Product")
                .capacityLiters(new BigDecimal("1.00"))
                .productType(ProductType.DISPOSABLE)
                .unitPrice(new BigDecimal("5.00"));

        assertThatThrownBy(() -> baseBuilder.sku(null).build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("SKU must not be null");

        assertThatThrownBy(() -> baseBuilder.sku("TEST-SKU").name(null).build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Name must not be null");

        assertThatThrownBy(() -> baseBuilder.name("Test Product").capacityLiters(null).build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Capacity in liters must not be null");

        assertThatThrownBy(() -> baseBuilder.capacityLiters(BigDecimal.ONE).productType(null).build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Product type must not be null");

        assertThatThrownBy(() -> baseBuilder.productType(ProductType.DISPOSABLE).unitPrice(null).build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Unit price must not be null");
    }

    @Test
    @DisplayName("Should reject unit price less than or equal to zero")
    void shouldRejectInvalidPrice() {
        Product.Builder baseBuilder = Product.builder()
                .sku("TEST-SKU")
                .name("Test Product")
                .capacityLiters(new BigDecimal("1.00"))
                .productType(ProductType.DISPOSABLE);

        assertThatThrownBy(() -> baseBuilder.unitPrice(BigDecimal.ZERO).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unit price must be greater than zero");

        assertThatThrownBy(() -> baseBuilder.unitPrice(new BigDecimal("-10.00")).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unit price must be greater than zero");
    }

    @Test
    @DisplayName("Should reject capacity in liters less than or equal to zero")
    void shouldRejectInvalidCapacity() {
        Product.Builder baseBuilder = Product.builder()
                .sku("TEST-SKU")
                .name("Test Product")
                .productType(ProductType.DISPOSABLE)
                .unitPrice(new BigDecimal("5.00"));

        assertThatThrownBy(() -> baseBuilder.capacityLiters(BigDecimal.ZERO).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Capacity in liters must be greater than zero");

        assertThatThrownBy(() -> baseBuilder.capacityLiters(new BigDecimal("-1.50")).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Capacity in liters must be greater than zero");
    }

    @Test
    @DisplayName("Should reject negative initial stock balances")
    void shouldRejectNegativeStock() {
        Product.Builder baseBuilder = Product.builder()
                .sku("TEST-SKU")
                .name("Test Product")
                .capacityLiters(new BigDecimal("1.00"))
                .productType(ProductType.DISPOSABLE)
                .unitPrice(new BigDecimal("5.00"));

        assertThatThrownBy(() -> baseBuilder.stockFull(-1).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Stock balances cannot be negative");

        assertThatThrownBy(() -> baseBuilder.stockFull(0).stockEmpty(-5).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Stock balances cannot be negative");
    }
}
