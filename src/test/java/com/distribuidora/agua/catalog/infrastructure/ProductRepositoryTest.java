package com.distribuidora.agua.catalog.infrastructure;

import com.distribuidora.agua.catalog.domain.Product;
import com.distribuidora.agua.catalog.domain.ProductType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("default")
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("Should save and retrieve product by SKU")
    void shouldSaveAndRetrieveBySku() {
        Product product = Product.builder()
                .sku("GALLON-20L-COMPLETE")
                .name("Mineral Water Gallon 20L Complete (Water + Bottle)")
                .capacityLiters(new BigDecimal("20.00"))
                .returnable(true)
                .productType(ProductType.FULL_WATER)
                .unitPrice(new BigDecimal("35.00"))
                .stockFull(40)
                .stockEmpty(10)
                .active(true)
                .build();

        Product saved = productRepository.save(product);
        assertThat(saved.getId()).isNotNull();

        Optional<Product> found = productRepository.findBySku("GALLON-20L-COMPLETE");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Mineral Water Gallon 20L Complete (Water + Bottle)");
        assertThat(found.get().getProductType()).isEqualTo(ProductType.FULL_WATER);
        assertThat(found.get().getVersion()).isNotNull();
    }

    @Test
    @DisplayName("Should list only active products")
    void shouldListOnlyActiveProducts() {
        Product p1 = Product.builder()
                .sku("BOTTLE-500ML")
                .name("Water Bottle 500ml")
                .capacityLiters(new BigDecimal("0.50"))
                .returnable(false)
                .productType(ProductType.DISPOSABLE)
                .unitPrice(new BigDecimal("2.50"))
                .stockFull(100)
                .stockEmpty(0)
                .active(true)
                .build();

        Product p2 = Product.builder()
                .sku("GALLON-10L-INACTIVE")
                .name("Gallon 10L Discontinued")
                .capacityLiters(new BigDecimal("10.00"))
                .returnable(true)
                .productType(ProductType.REFILL)
                .unitPrice(new BigDecimal("12.00"))
                .stockFull(0)
                .stockEmpty(0)
                .active(false)
                .build();

        productRepository.save(p1);
        productRepository.save(p2);

        List<Product> activeProducts = productRepository.findByActiveTrue();
        assertThat(activeProducts).extracting(Product::getSku).contains("BOTTLE-500ML");
        assertThat(activeProducts).extracting(Product::getSku).doesNotContain("GALLON-10L-INACTIVE");
    }

    @Test
    @DisplayName("Should detect optimistic locking conflict with @Version")
    void shouldDetectOptimisticLockingConflict() {
        Product product = Product.builder()
                .sku("GALLON-20L-LOCK")
                .name("Gallon 20L Concurrency Test")
                .capacityLiters(new BigDecimal("20.00"))
                .returnable(true)
                .productType(ProductType.FULL_WATER)
                .unitPrice(new BigDecimal("30.00"))
                .stockFull(10)
                .stockEmpty(5)
                .active(true)
                .build();

        Product saved = productRepository.saveAndFlush(product);
        Long id = saved.getId();

        // Load first instance and detach from persistence context
        Product instance1 = productRepository.findById(id).orElseThrow();
        entityManager.detach(instance1);

        // Load second instance and perform concurrent update
        Product instance2 = productRepository.findById(id).orElseThrow();
        instance2.setStockFull(15);
        productRepository.saveAndFlush(instance2);

        // Clear persistence context to force merge on outdated instance 1
        entityManager.clear();

        // Instance 1 has outdated version (version = 0, while in DB version is now 1)
        instance1.setStockFull(20);
        assertThatThrownBy(() -> productRepository.saveAndFlush(instance1))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }
}
