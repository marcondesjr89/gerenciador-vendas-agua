package com.distribuidora.agua.payment.infrastructure;

import com.distribuidora.agua.orders.domain.Modality;
import com.distribuidora.agua.orders.domain.Order;
import com.distribuidora.agua.orders.domain.OrderStatus;
import com.distribuidora.agua.orders.domain.PaymentMethod;
import com.distribuidora.agua.orders.infrastructure.OrderRepository;
import com.distribuidora.agua.payment.domain.Payment;
import com.distribuidora.agua.payment.domain.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@DisplayName("PaymentRepository Persistence & Unique Constraint Tests")
class PaymentRepositoryTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Order order;

    @BeforeEach
    void setUp() {
        order = Order.builder()
                .orderCode("ORD-PAY-TEST-001")
                .customerName("Eduardo Rocha")
                .customerPhone("11944443333")
                .modality(Modality.PICKUP)
                .paymentMethod(PaymentMethod.PIX)
                .totalAmount(new BigDecimal("30.00"))
                .status(OrderStatus.PENDING)
                .build();
        order = orderRepository.save(order);
    }

    @Test
    @DisplayName("Should persist payment and retrieve by idempotency key")
    void shouldPersistAndFindByIdempotencyKey() {
        Payment payment = new Payment(
                order,
                "idemp-jpa-unique-1",
                PaymentMethod.PIX,
                new BigDecimal("30.00"),
                PaymentStatus.PAID
        );

        Payment saved = paymentRepository.save(payment);
        entityManager.flush();
        entityManager.clear();

        Optional<Payment> found = paymentRepository.findByIdempotencyKey("idemp-jpa-unique-1");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isNotNull();
        assertThat(found.get().getOrder().getId()).isEqualTo(order.getId());
        assertThat(found.get().getPaidAmount()).isEqualByComparingTo("30.00");
        assertThat(found.get().getStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    @DisplayName("Should enforce unique constraint on idempotency_key in database")
    void shouldEnforceUniqueConstraintOnIdempotencyKey() {
        Payment payment1 = new Payment(
                order,
                "duplicate-idemp-key",
                PaymentMethod.PIX,
                new BigDecimal("30.00"),
                PaymentStatus.PAID
        );
        paymentRepository.save(payment1);
        entityManager.flush();

        Payment payment2 = new Payment(
                order,
                "duplicate-idemp-key",
                PaymentMethod.CASH,
                new BigDecimal("30.00"),
                PaymentStatus.PAID
        );

        assertThatThrownBy(() -> {
            paymentRepository.save(payment2);
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}
