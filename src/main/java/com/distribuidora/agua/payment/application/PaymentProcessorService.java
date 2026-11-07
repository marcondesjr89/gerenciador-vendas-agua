package com.distribuidora.agua.payment.application;

import com.distribuidora.agua.orders.application.OrderService;
import com.distribuidora.agua.orders.domain.Order;
import com.distribuidora.agua.orders.domain.OrderStatus;
import com.distribuidora.agua.payment.domain.Payment;
import com.distribuidora.agua.payment.domain.PaymentStatus;
import com.distribuidora.agua.payment.infrastructure.PaymentRepository;
import com.distribuidora.agua.platform.api.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Service managing idempotent payment processing with dual protection (Redis lock + PostgreSQL unique constraint).
 */
@Service
public class PaymentProcessorService {

    private static final Logger log = LoggerFactory.getLogger(PaymentProcessorService.class);

    private static final Duration LOCK_EXPIRATION = Duration.ofSeconds(15);
    private static final Duration IDEMPOTENCY_CACHE_TTL = Duration.ofHours(24);

    private final PaymentRepository paymentRepository;
    private final OrderService orderService;
    private final Optional<StringRedisTemplate> redisTemplate;

    public PaymentProcessorService(
            PaymentRepository paymentRepository,
            OrderService orderService,
            Optional<StringRedisTemplate> redisTemplate
    ) {
        this.paymentRepository = paymentRepository;
        this.orderService = orderService;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Checks if the given idempotency key has already been processed and persisted.
     */
    public boolean isReplay(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return false;
        }
        return paymentRepository.existsByIdempotencyKey(idempotencyKey.trim());
    }

    /**
     * Atomically processes a payment verifying idempotency in Redis and PostgreSQL.
     */
    @Transactional
    public Payment processPayment(String rawIdempotencyKey, ProcessPaymentCommand command) {
        Objects.requireNonNull(rawIdempotencyKey, "Idempotency-Key header cannot be null");
        Objects.requireNonNull(command, "ProcessPaymentCommand cannot be null");

        String idempotencyKey = rawIdempotencyKey.trim();

        // 1. Database fast-path replay check
        Optional<Payment> existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.info("Idempotent payment replay detected for key [{}]. Returning existing payment [{}]",
                    idempotencyKey, existing.get().getId());
            return existing.get();
        }

        // 2. Redis distributed concurrency lock
        acquireDistributedLock(idempotencyKey);

        // 3. Re-check DB in case another thread just completed during lock acquisition
        Optional<Payment> rechecked = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (rechecked.isPresent()) {
            return rechecked.get();
        }

        // 4. Validate order and amount
        Order order = orderService.findById(command.orderId());
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Nao e possivel processar pagamento para pedido cancelado: " + order.getOrderCode());
        }

        if (command.paidAmount().compareTo(order.getTotalAmount()) < 0) {
            throw new IllegalArgumentException(String.format(
                    "Valor pago insuficiente: R$ %s (total do pedido: R$ %s)",
                    command.paidAmount(), order.getTotalAmount()
            ));
        }

        // 5. Advance order status if pending
        if (order.getStatus() == OrderStatus.PENDING) {
            orderService.updateStatus(order.getId(), OrderStatus.CONFIRMED);
        }

        // 6. Persist payment
        Payment payment = new Payment(
                order,
                idempotencyKey,
                command.paymentMethod(),
                command.paidAmount(),
                PaymentStatus.PAID
        );

        Payment savedPayment;
        try {
            savedPayment = paymentRepository.save(payment);
            log.info("Successfully processed payment [{}] for order [{}] with idempotency key [{}]",
                    savedPayment.getId(), order.getOrderCode(), idempotencyKey);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Database unique constraint caught concurrent payment duplicate for key [{}]", idempotencyKey);
            return paymentRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> ex);
        }

        // 7. Store idempotency confirmation in Redis
        cacheIdempotencyCompletion(idempotencyKey, savedPayment.getId());

        return savedPayment;
    }

    /**
     * Finds payment by primary key ID.
     */
    @Transactional(readOnly = true)
    public Payment findById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento nao encontrado com id: " + id));
    }

    /**
     * Finds payments associated with a specific order.
     */
    @Transactional(readOnly = true)
    public List<Payment> findByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId);
    }

    private void acquireDistributedLock(String idempotencyKey) {
        redisTemplate.ifPresent(rt -> {
            try {
                String lockKey = "lock:payment:" + idempotencyKey;
                Boolean acquired = rt.opsForValue().setIfAbsent(lockKey, "LOCKED", LOCK_EXPIRATION);
                if (Boolean.FALSE.equals(acquired)) {
                    log.warn("Concurrent lock contention on Redis for key [{}]. Yielding execution", idempotencyKey);
                    try {
                        Thread.sleep(80);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            } catch (Exception ex) {
                log.warn("Redis lock operation failed for key [{}]. Falling back to PostgreSQL unique constraint: {}",
                        idempotencyKey, ex.getMessage());
            }
        });
    }

    private void cacheIdempotencyCompletion(String idempotencyKey, Long paymentId) {
        redisTemplate.ifPresent(rt -> {
            try {
                String cacheKey = "idempotency:payment:" + idempotencyKey;
                rt.opsForValue().set(cacheKey, paymentId.toString(), IDEMPOTENCY_CACHE_TTL);
            } catch (Exception ex) {
                log.warn("Failed to cache completed idempotency key [{}] in Redis: {}", idempotencyKey, ex.getMessage());
            }
        });
    }
}
