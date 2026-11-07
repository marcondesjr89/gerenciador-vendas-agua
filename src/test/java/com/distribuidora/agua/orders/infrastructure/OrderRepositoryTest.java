package com.distribuidora.agua.orders.infrastructure;

import com.distribuidora.agua.catalog.domain.Product;
import com.distribuidora.agua.catalog.domain.ProductType;
import com.distribuidora.agua.catalog.infrastructure.ProductRepository;
import com.distribuidora.agua.orders.domain.Modality;
import com.distribuidora.agua.orders.domain.Order;
import com.distribuidora.agua.orders.domain.OrderStatus;
import com.distribuidora.agua.orders.domain.PaymentMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("OrderRepository Persistence & Integration Tests")
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Product product;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .sku("GAL-20L-TEST")
                .name("Galao 20L Persist")
                .productType(ProductType.FULL_WATER)
                .unitPrice(new BigDecimal("32.00"))
                .capacityLiters(new BigDecimal("20.00"))
                .stockFull(40)
                .stockEmpty(15)
                .returnable(true)
                .active(true)
                .build();
        product = productRepository.save(product);
    }

    @Test
    @DisplayName("Should persist order with cascading order items and find by order code")
    void shouldPersistOrderAndItemsCascade() {
        Order order = Order.builder()
                .orderCode("ORD-TEST-0001")
                .customerName("Roberto Dias")
                .customerPhone("11955554444")
                .modality(Modality.DELIVERY)
                .paymentMethod(PaymentMethod.PIX)
                .deliveryFee(new BigDecimal("5.00"))
                .build();

        order.addItem(product, 2, false);
        order.recalculateTotals();

        Order saved = orderRepository.save(order);
        entityManager.flush();
        entityManager.clear();

        Optional<Order> found = orderRepository.findByOrderCode("ORD-TEST-0001");

        assertThat(found).isPresent();
        Order loaded = found.get();
        assertThat(loaded.getId()).isNotNull();
        assertThat(loaded.getCustomerName()).isEqualTo("Roberto Dias");
        assertThat(loaded.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(loaded.getItems()).hasSize(1);
        assertThat(loaded.getItems().get(0).getProduct().getSku()).isEqualTo("GAL-20L-TEST");
        assertThat(loaded.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(loaded.getSubtotal()).isEqualByComparingTo("64.00");
        assertThat(loaded.getTotalAmount()).isEqualByComparingTo("69.00");
    }

    @Test
    @DisplayName("Should find orders by customer phone")
    void shouldFindOrdersByCustomerPhone() {
        Order order1 = Order.builder()
                .orderCode("ORD-TEST-0002")
                .customerName("Julia Lima")
                .customerPhone("11912345678")
                .modality(Modality.PICKUP)
                .paymentMethod(PaymentMethod.CASH)
                .build();
        order1.addItem(product, 1, false);
        order1.recalculateTotals();

        orderRepository.save(order1);
        entityManager.flush();
        entityManager.clear();

        var orders = orderRepository.findByCustomerPhone("11912345678");
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getOrderCode()).isEqualTo("ORD-TEST-0002");
    }
}
