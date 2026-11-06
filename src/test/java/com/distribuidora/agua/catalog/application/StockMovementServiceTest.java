package com.distribuidora.agua.catalog.application;

import com.distribuidora.agua.catalog.domain.DisposableStockStrategy;
import com.distribuidora.agua.catalog.domain.EmptyBottleStockStrategy;
import com.distribuidora.agua.catalog.domain.FullWaterStockStrategy;
import com.distribuidora.agua.catalog.domain.Product;
import com.distribuidora.agua.catalog.domain.ProductType;
import com.distribuidora.agua.catalog.domain.RefillStockStrategy;
import com.distribuidora.agua.catalog.domain.StockMovementStrategy;
import com.distribuidora.agua.catalog.infrastructure.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StockMovementServiceTest {

    @Mock
    private ProductRepository productRepository;

    private StockMovementService stockMovementService;
    private Product product;

    @BeforeEach
    void setUp() {
        List<StockMovementStrategy> strategies = List.of(
                new FullWaterStockStrategy(),
                new RefillStockStrategy(),
                new EmptyBottleStockStrategy(),
                new DisposableStockStrategy()
        );
        stockMovementService = new StockMovementService(strategies, productRepository);

        product = Product.builder()
                .id(10L)
                .sku("REFILL-20L")
                .name("Refill Mineral Water Gallon 20L")
                .capacityLiters(new BigDecimal("20.00"))
                .returnable(true)
                .productType(ProductType.REFILL)
                .unitPrice(new BigDecimal("15.00"))
                .stockFull(20)
                .stockEmpty(10)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Should successfully debit stock delegating to the appropriate strategy")
    void shouldDebitStock() {
        stockMovementService.debitStock(product, 5);

        assertThat(product.getStockFull()).isEqualTo(15);
        assertThat(product.getStockEmpty()).isEqualTo(15);
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("Should successfully credit stock on refund delegating to the strategy")
    void shouldCreditStock() {
        stockMovementService.creditStock(product, 2);

        assertThat(product.getStockFull()).isEqualTo(22);
        assertThat(product.getStockEmpty()).isEqualTo(8);
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("Should fail when trying to move quantity less than or equal to zero")
    void shouldFailInvalidQuantity() {
        assertThatThrownBy(() -> stockMovementService.debitStock(product, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quantity must be greater than zero");

        assertThatThrownBy(() -> stockMovementService.creditStock(product, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quantity must be greater than zero");
    }

    @Test
    @DisplayName("Should adjust stock directly with valid values")
    void shouldAdjustStockDirectly() {
        stockMovementService.adjustStock(product, 50, 30);

        assertThat(product.getStockFull()).isEqualTo(50);
        assertThat(product.getStockEmpty()).isEqualTo(30);
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("Should allow adjusting stock to valid zero lower boundary")
    void shouldAllowAdjustingStockToZero() {
        stockMovementService.adjustStock(product, 0, 0);

        assertThat(product.getStockFull()).isZero();
        assertThat(product.getStockEmpty()).isZero();
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("Should fail when adjusting stock with negative values")
    void shouldFailAdjustStockNegative() {
        assertThatThrownBy(() -> stockMovementService.adjustStock(product, -5, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Stock balances cannot be negative");

        assertThatThrownBy(() -> stockMovementService.adjustStock(product, 10, -2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Stock balances cannot be negative");
    }

    @Test
    @DisplayName("Should throw IllegalStateException if no strategy is found for ProductType")
    void shouldFailWhenStrategyNotFound() {
        StockMovementService serviceWithoutStrategies = new StockMovementService(Collections.emptyList(), productRepository);

        assertThatThrownBy(() -> serviceWithoutStrategies.debitStock(product, 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No stock strategy found");
    }
}
