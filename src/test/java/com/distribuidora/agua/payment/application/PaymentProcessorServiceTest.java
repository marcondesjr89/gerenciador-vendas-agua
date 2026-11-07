package com.distribuidora.agua.payment.application;

import com.distribuidora.agua.orders.application.OrderService;
import com.distribuidora.agua.orders.domain.Modality;
import com.distribuidora.agua.orders.domain.Order;
import com.distribuidora.agua.orders.domain.OrderStatus;
import com.distribuidora.agua.orders.domain.PaymentMethod;
import com.distribuidora.agua.payment.domain.Payment;
import com.distribuidora.agua.payment.domain.PaymentStatus;
import com.distribuidora.agua.payment.infrastructure.PaymentRepository;
import com.distribuidora.agua.platform.api.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentProcessorService Unit & Concurrency Tests")
class PaymentProcessorServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderService orderService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private com.distribuidora.agua.platform.observability.RedMetricsService redMetricsService;

    private PaymentProcessorService paymentProcessorService;

    private Order sampleOrder;

    @BeforeEach
    void setUp() {
        paymentProcessorService = new PaymentProcessorService(
                paymentRepository,
                orderService,
                Optional.of(redisTemplate),
                redMetricsService
        );

        sampleOrder = Order.builder()
                .id(1L)
                .orderCode("ORD-20261107-000001")
                .customerName("Ana Clara")
                .customerPhone("11988881234")
                .modality(Modality.DELIVERY)
                .paymentMethod(PaymentMethod.PIX)
                .subtotal(new BigDecimal("60.00"))
                .deliveryFee(new BigDecimal("5.00"))
                .totalAmount(new BigDecimal("65.00"))
                .status(OrderStatus.PENDING)
                .build();
    }

    @Test
    @DisplayName("Should successfully process payment on first attempt and advance order to CONFIRMED")
    void shouldProcessPaymentSuccessfully() {
        String idempotencyKey = "idemp-key-001";
        ProcessPaymentCommand command = new ProcessPaymentCommand(
                1L,
                PaymentMethod.PIX,
                new BigDecimal("65.00")
        );

        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(orderService.findById(1L)).thenReturn(sampleOrder);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            return new Payment(
                    10L,
                    p.getOrder(),
                    p.getIdempotencyKey(),
                    p.getPaymentMethod(),
                    p.getPaidAmount(),
                    p.getStatus(),
                    p.getProcessedAt()
            );
        });

        Payment payment = paymentProcessorService.processPayment(idempotencyKey, command);

        assertThat(payment).isNotNull();
        assertThat(payment.getId()).isEqualTo(10L);
        assertThat(payment.getIdempotencyKey()).isEqualTo(idempotencyKey);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getPaidAmount()).isEqualByComparingTo("65.00");

        verify(orderService).updateStatus(1L, OrderStatus.CONFIRMED);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    @DisplayName("Should return existing payment when idempotency key was already processed (replay)")
    void shouldReturnExistingPaymentOnIdempotentReplay() {
        String idempotencyKey = "idemp-key-duplicate";
        ProcessPaymentCommand command = new ProcessPaymentCommand(
                1L,
                PaymentMethod.PIX,
                new BigDecimal("65.00")
        );

        Payment existingPayment = new Payment(
                55L,
                sampleOrder,
                idempotencyKey,
                PaymentMethod.PIX,
                new BigDecimal("65.00"),
                PaymentStatus.PAID,
                Instant.now()
        );

        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(existingPayment));

        Payment replayed = paymentProcessorService.processPayment(idempotencyKey, command);

        assertThat(replayed).isSameAs(existingPayment);
        assertThat(replayed.getId()).isEqualTo(55L);

        // Crucial: ensure no new save or status change occurs
        verify(paymentRepository, never()).save(any());
        verify(orderService, never()).updateStatus(any(), any());
    }

    @Test
    @DisplayName("Should reject payment when order is already CANCELLED")
    void shouldRejectPaymentForCancelledOrder() {
        String idempotencyKey = "idemp-key-cancelled";
        sampleOrder.cancel();

        ProcessPaymentCommand command = new ProcessPaymentCommand(
                1L,
                PaymentMethod.PIX,
                new BigDecimal("65.00")
        );

        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(orderService.findById(1L)).thenReturn(sampleOrder);

        assertThatThrownBy(() -> paymentProcessorService.processPayment(idempotencyKey, command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cancelado");

        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject payment when paid amount is less than order total amount")
    void shouldRejectInsufficientPaymentAmount() {
        String idempotencyKey = "idemp-key-insufficient";
        ProcessPaymentCommand command = new ProcessPaymentCommand(
                1L,
                PaymentMethod.PIX,
                new BigDecimal("30.00") // total is 65.00
        );

        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(orderService.findById(1L)).thenReturn(sampleOrder);

        assertThatThrownBy(() -> paymentProcessorService.processPayment(idempotencyKey, command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Valor pago insuficiente");

        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Concurrency test: multiple concurrent threads with same idempotency key process exactly once")
    void shouldEnsureSingleExecutionUnderConcurrentRequests() throws Exception {
        String idempotencyKey = "concurrent-key-123";
        ProcessPaymentCommand command = new ProcessPaymentCommand(
                1L,
                PaymentMethod.PIX,
                new BigDecimal("65.00")
        );

        Payment savedPayment = new Payment(
                99L,
                sampleOrder,
                idempotencyKey,
                PaymentMethod.PIX,
                new BigDecimal("65.00"),
                PaymentStatus.PAID,
                Instant.now()
        );

        when(orderService.findById(1L)).thenReturn(sampleOrder);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        java.util.concurrent.atomic.AtomicReference<Payment> stored = new java.util.concurrent.atomic.AtomicReference<>(null);
        AtomicInteger saveCalls = new AtomicInteger(0);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            saveCalls.incrementAndGet();
            stored.set(savedPayment);
            return savedPayment;
        });

        // First thread acquires lock, subsequent thread gets lock contention
        when(valueOperations.setIfAbsent(eq("lock:payment:" + idempotencyKey), eq("LOCKED"), any(Duration.class)))
                .thenReturn(true)
                .thenReturn(false);

        when(paymentRepository.findByIdempotencyKey(idempotencyKey))
                .thenAnswer(inv -> Optional.ofNullable(stored.get()));

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);

        Future<Payment> f1 = executor.submit(() -> {
            latch.await();
            return paymentProcessorService.processPayment(idempotencyKey, command);
        });
        Future<Payment> f2 = executor.submit(() -> {
            latch.await();
            return paymentProcessorService.processPayment(idempotencyKey, command);
        });

        latch.countDown();

        Payment p1 = f1.get();
        Payment p2 = f2.get();

        assertThat(p1).isNotNull();
        assertThat(p2).isNotNull();
        assertThat(p1.getId()).isEqualTo(p2.getId());
        assertThat(saveCalls.get()).isEqualTo(1);

        executor.shutdown();
    }
}
