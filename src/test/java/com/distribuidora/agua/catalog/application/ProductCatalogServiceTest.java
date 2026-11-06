package com.distribuidora.agua.catalog.application;

import com.distribuidora.agua.catalog.api.ProductRequest;
import com.distribuidora.agua.catalog.api.ProductResponse;
import com.distribuidora.agua.catalog.api.StockAdjustmentRequest;
import com.distribuidora.agua.catalog.domain.Product;
import com.distribuidora.agua.catalog.domain.ProductType;
import com.distribuidora.agua.catalog.infrastructure.ProductRepository;
import com.distribuidora.agua.platform.api.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductCatalogServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockMovementService stockMovementService;

    @InjectMocks
    private ProductCatalogService catalogService;

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
                .stockFull(100)
                .stockEmpty(40)
                .active(true)
                .version(0L)
                .build();
    }

    @Test
    @DisplayName("Should list active products correctly")
    void shouldListActiveProducts() {
        when(productRepository.findByActiveTrue()).thenReturn(List.of(product));

        List<ProductResponse> products = catalogService.listActiveProducts();

        assertThat(products).hasSize(1);
        assertThat(products.get(0).sku()).isEqualTo("GALLON-20L");
        assertThat(products.get(0).unitPrice()).isEqualByComparingTo("30.00");
    }

    @Test
    @DisplayName("Should retrieve product by ID when it exists")
    void shouldGetProductById() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductResponse response = catalogService.getProductById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.sku()).isEqualTo("GALLON-20L");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when product does not exist")
    void shouldThrowWhenProductNotFound() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> catalogService.getProductById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found with ID: 999");
    }

    @Test
    @DisplayName("Should create product successfully when SKU is unique")
    void shouldCreateProduct() {
        ProductRequest request = new ProductRequest(
                "GALLON-10L",
                "Mineral Water 10L",
                new BigDecimal("10.00"),
                true,
                ProductType.REFILL,
                new BigDecimal("18.00"),
                50,
                20
        );

        when(productRepository.findBySku("GALLON-10L")).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product toSave = invocation.getArgument(0);
            return Product.builder()
                    .id(2L)
                    .sku(toSave.getSku())
                    .name(toSave.getName())
                    .capacityLiters(toSave.getCapacityLiters())
                    .returnable(toSave.isReturnable())
                    .productType(toSave.getProductType())
                    .unitPrice(toSave.getUnitPrice())
                    .stockFull(toSave.getStockFull())
                    .stockEmpty(toSave.getStockEmpty())
                    .active(true)
                    .version(0L)
                    .build();
        });

        ProductResponse response = catalogService.createProduct(request);

        assertThat(response.id()).isEqualTo(2L);
        assertThat(response.sku()).isEqualTo("GALLON-10L");
        assertThat(response.productType()).isEqualTo(ProductType.REFILL);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when creating product with duplicate SKU")
    void shouldThrowDuplicateSku() {
        ProductRequest request = new ProductRequest(
                "GALLON-20L",
                "Duplicate",
                new BigDecimal("20.00"),
                true,
                ProductType.FULL_WATER,
                new BigDecimal("30.00"),
                10,
                5
        );

        when(productRepository.findBySku("GALLON-20L")).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> catalogService.createProduct(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product with SKU already exists: GALLON-20L");
    }

    @Test
    @DisplayName("Should adjust stock balances delegating to StockMovementService")
    void shouldAdjustStock() {
        StockAdjustmentRequest request = new StockAdjustmentRequest(120, 50);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductResponse response = catalogService.adjustStock(1L, request);

        verify(stockMovementService).adjustStock(product, 120, 50);
        assertThat(response.id()).isEqualTo(1L);
    }
}
