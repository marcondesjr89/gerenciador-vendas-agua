package com.distribuidora.agua.orders.infrastructure;

import com.distribuidora.agua.orders.domain.Order;
import com.distribuidora.agua.orders.domain.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Order aggregate.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderCode(String orderCode);

    List<Order> findByCustomerPhone(String customerPhone);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    Page<Order> findByCustomerPhone(String customerPhone, Pageable pageable);

    Page<Order> findByStatusAndCustomerPhone(OrderStatus status, String customerPhone, Pageable pageable);
}
