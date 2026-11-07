package com.distribuidora.agua.payment.infrastructure;

import com.distribuidora.agua.payment.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Payment aggregate.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    List<Payment> findByOrderId(Long orderId);

    boolean existsByIdempotencyKey(String idempotencyKey);
}
