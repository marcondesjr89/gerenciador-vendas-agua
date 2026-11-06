package com.distribuidora.agua.catalog.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockMovementStrategyTest {

    private Product product;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .id(1L)
                .sku("GALLON-20L")
                .name("Mineral Water Gallon 20L")
                .capacityLiters(new BigDecimal("20.00"))
                .returnable(true)
                .productType(ProductType.FULL_WATER)
                .unitPrice(new BigDecimal("30.00"))
                .stockFull(10)
                .stockEmpty(5)
                .active(true)
                .build();
    }

    @Nested
    @DisplayName("Tests for FullWaterStockStrategy")
    class FullWaterTests {
        private final StockMovementStrategy strategy = new FullWaterStockStrategy();

        @Test
        @DisplayName("Should support only ProductType.FULL_WATER")
        void shouldSupportCorrectType() {
            assertThat(strategy.supports(ProductType.FULL_WATER)).isTrue();
            assertThat(strategy.supports(ProductType.REFILL)).isFalse();
        }

        @Test
        @DisplayName("Should debit only full units stock")
        void shouldDebitFullUnits() {
            strategy.debitStock(product, 3);
            assertThat(product.getStockFull()).isEqualTo(7);
            assertThat(product.getStockEmpty()).isEqualTo(5);
        }

        @Test
        @DisplayName("Should zero stock when quantity matches current balance exactly")
        void shouldZeroStockOnExactBoundary() {
            strategy.debitStock(product, 10);
            assertThat(product.getStockFull()).isZero();
        }

        @Test
        @DisplayName("Should credit only full units stock")
        void shouldCreditFullUnits() {
            strategy.creditStock(product, 2);
            assertThat(product.getStockFull()).isEqualTo(12);
            assertThat(product.getStockEmpty()).isEqualTo(5);
        }

        @Test
        @DisplayName("Should throw InsufficientStockException if full units are insufficient")
        void shouldThrowExceptionWhenInsufficientStock() {
            assertThatThrownBy(() -> strategy.debitStock(product, 15))
                    .isInstanceOf(InsufficientStockException.class)
                    .hasMessageContaining("Insufficient full units stock");
        }

        @Test
        @DisplayName("Should throw InsufficientStockException if full stock is already zero")
        void shouldThrowExceptionWhenStockAlreadyZero() {
            product.setStockFull(0);
            assertThatThrownBy(() -> strategy.debitStock(product, 1))
                    .isInstanceOf(InsufficientStockException.class)
                    .hasMessageContaining("Insufficient full units stock");
        }
    }

    @Nested
    @DisplayName("Tests for RefillStockStrategy")
    class RefillTests {
        private final StockMovementStrategy strategy = new RefillStockStrategy();

        @Test
        @DisplayName("Should support only ProductType.REFILL")
        void shouldSupportCorrectType() {
            assertThat(strategy.supports(ProductType.REFILL)).isTrue();
            assertThat(strategy.supports(ProductType.FULL_WATER)).isFalse();
        }

        @Test
        @DisplayName("Should debit full units and credit empty bottles on refill sale")
        void shouldDebitFullAndCreditEmpty() {
            strategy.debitStock(product, 4);
            assertThat(product.getStockFull()).isEqualTo(6);
            assertThat(product.getStockEmpty()).isEqualTo(9); // 5 + 4
        }

        @Test
        @DisplayName("Should credit full units and debit empty bottles on refill refund")
        void shouldRefundRefill() {
            strategy.creditStock(product, 3);
            assertThat(product.getStockFull()).isEqualTo(13); // 10 + 3
            assertThat(product.getStockEmpty()).isEqualTo(2);  // 5 - 3
        }

        @Test
        @DisplayName("Should maintain zero floor for empty bottles on refund if current balance is lower")
        void shouldMaintainZeroFloorOnRefund() {
            product.setStockEmpty(1);
            strategy.creditStock(product, 5);
            assertThat(product.getStockFull()).isEqualTo(15);
            assertThat(product.getStockEmpty()).isZero();
        }

        @Test
        @DisplayName("Should throw InsufficientStockException if full units are insufficient")
        void shouldThrowExceptionWhenFullStockInsufficient() {
            assertThatThrownBy(() -> strategy.debitStock(product, 20))
                    .isInstanceOf(InsufficientStockException.class)
                    .hasMessageContaining("Insufficient full units stock");
        }
    }

    @Nested
    @DisplayName("Tests for EmptyBottleStockStrategy")
    class EmptyBottleTests {
        private final StockMovementStrategy strategy = new EmptyBottleStockStrategy();

        @Test
        @DisplayName("Should support only ProductType.EMPTY_BOTTLE")
        void shouldSupportCorrectType() {
            assertThat(strategy.supports(ProductType.EMPTY_BOTTLE)).isTrue();
            assertThat(strategy.supports(ProductType.DISPOSABLE)).isFalse();
        }

        @Test
        @DisplayName("Should debit only empty bottles stock")
        void shouldDebitEmptyBottles() {
            strategy.debitStock(product, 2);
            assertThat(product.getStockFull()).isEqualTo(10);
            assertThat(product.getStockEmpty()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should zero empty bottles stock on exact boundary")
        void shouldZeroEmptyBottlesOnExactBoundary() {
            strategy.debitStock(product, 5);
            assertThat(product.getStockEmpty()).isZero();
        }

        @Test
        @DisplayName("Should credit only empty bottles stock on refund")
        void shouldCreditEmptyBottles() {
            strategy.creditStock(product, 4);
            assertThat(product.getStockFull()).isEqualTo(10);
            assertThat(product.getStockEmpty()).isEqualTo(9);
        }

        @Test
        @DisplayName("Should throw InsufficientStockException if empty bottles are insufficient")
        void shouldThrowExceptionWhenEmptyBottlesInsufficient() {
            assertThatThrownBy(() -> strategy.debitStock(product, 10))
                    .isInstanceOf(InsufficientStockException.class)
                    .hasMessageContaining("Insufficient empty bottles stock");
        }
    }

    @Nested
    @DisplayName("Tests for DisposableStockStrategy")
    class DisposableTests {
        private final StockMovementStrategy strategy = new DisposableStockStrategy();

        @Test
        @DisplayName("Should support only ProductType.DISPOSABLE")
        void shouldSupportCorrectType() {
            assertThat(strategy.supports(ProductType.DISPOSABLE)).isTrue();
            assertThat(strategy.supports(ProductType.REFILL)).isFalse();
        }

        @Test
        @DisplayName("Should debit full units without changing empty bottles")
        void shouldDebitFullUnitsWithoutChangingEmpty() {
            strategy.debitStock(product, 5);
            assertThat(product.getStockFull()).isEqualTo(5);
            assertThat(product.getStockEmpty()).isEqualTo(5);
        }

        @Test
        @DisplayName("Should credit full units without changing empty bottles on refund")
        void shouldCreditFullUnits() {
            strategy.creditStock(product, 5);
            assertThat(product.getStockFull()).isEqualTo(15);
            assertThat(product.getStockEmpty()).isEqualTo(5);
        }

        @Test
        @DisplayName("Should throw InsufficientStockException if full units are insufficient")
        void shouldThrowExceptionWhenInsufficientStock() {
            assertThatThrownBy(() -> strategy.debitStock(product, 12))
                    .isInstanceOf(InsufficientStockException.class)
                    .hasMessageContaining("Insufficient full units stock");
        }
    }
}
