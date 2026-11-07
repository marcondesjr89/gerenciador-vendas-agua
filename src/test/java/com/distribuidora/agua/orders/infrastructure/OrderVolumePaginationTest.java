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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("Order Volume & Composite Index Pagination Integration Tests")
class OrderVolumePaginationTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TestEntityManager entityManager;

    private static final int BATCH_SIZE = 150;
    private final String TARGET_PHONE = "11988880001";

    @BeforeEach
    void setUp() {
        Product product = Product.builder()
                .sku("GAL-20L-VOL")
                .name("Galao 20L Volume")
                .productType(ProductType.FULL_WATER)
                .unitPrice(new BigDecimal("30.00"))
                .capacityLiters(new BigDecimal("20.00"))
                .stockFull(1000)
                .stockEmpty(500)
                .returnable(true)
                .active(true)
                .build();
        Product savedProduct = productRepository.save(product);

        List<Order> orders = new ArrayList<>(BATCH_SIZE);
        Instant now = Instant.now();

        for (int i = 1; i <= BATCH_SIZE; i++) {
            OrderStatus status = switch (i % 4) {
                case 0 -> OrderStatus.CANCELLED;
                case 1 -> OrderStatus.DELIVERED;
                case 2 -> OrderStatus.CONFIRMED;
                default -> OrderStatus.IN_TRANSIT;
            };

            String phone = (i % 10 == 0) ? TARGET_PHONE : "1197777" + String.format("%04d", i);

            Order order = Order.builder()
                    .orderCode("ORD-VOL-" + String.format("%05d", i))
                    .customerName("Cliente Vol " + i)
                    .customerPhone(phone)
                    .modality(i % 2 == 0 ? Modality.DELIVERY : Modality.PICKUP)
                    .paymentMethod(PaymentMethod.PIX)
                    .status(status)
                    .deliveryFee(new BigDecimal("5.00"))
                    .createdAt(now.minusSeconds(i * 60L))
                    .build();

            order.addItem(savedProduct, 1, false);
            order.recalculateTotals();
            orders.add(order);
        }

        orderRepository.saveAll(orders);
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("Should query paginated orders by status sorted by createdAt DESC well within SLO")
    void shouldQueryPaginatedOrdersByStatusWithinSlo() {
        PageRequest pageRequest = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));

        Instant start = Instant.now();
        Page<Order> page = orderRepository.findByStatus(OrderStatus.DELIVERED, pageRequest);
        Instant end = Instant.now();

        long latencyMs = Duration.between(start, end).toMillis();

        assertThat(latencyMs).isLessThan(200); // SLO requirement: p95 < 200ms
        assertThat(page.getContent()).isNotEmpty();
        assertThat(page.getContent().size()).isLessThanOrEqualTo(20);
        assertThat(page.getContent()).allMatch(o -> o.getStatus() == OrderStatus.DELIVERED);

        // Verify descending order
        List<Order> content = page.getContent();
        for (int i = 0; i < content.size() - 1; i++) {
            assertThat(content.get(i).getCreatedAt())
                    .isAfterOrEqualTo(content.get(i + 1).getCreatedAt());
        }
    }

    @Test
    @DisplayName("Should query paginated orders by customer phone")
    void shouldQueryPaginatedOrdersByCustomerPhone() {
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<Order> page = orderRepository.findByCustomerPhone(TARGET_PHONE, pageRequest);

        assertThat(page.getContent()).isNotEmpty();
        assertThat(page.getContent()).allMatch(o -> o.getCustomerPhone().equals(TARGET_PHONE));
        assertThat(page.getTotalElements()).isEqualTo(BATCH_SIZE / 10);
    }

    @Test
    @DisplayName("Should query paginated orders by composite status and customer phone")
    void shouldQueryPaginatedOrdersByCompositeStatusAndPhone() {
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<Order> page = orderRepository.findByStatusAndCustomerPhone(
                OrderStatus.CANCELLED, TARGET_PHONE, pageRequest
        );

        assertThat(page.getContent()).allMatch(o ->
                o.getStatus() == OrderStatus.CANCELLED && o.getCustomerPhone().equals(TARGET_PHONE)
        );
    }
}
