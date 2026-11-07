package com.distribuidora.agua.orders.application;

import com.distribuidora.agua.catalog.application.StockMovementService;
import com.distribuidora.agua.catalog.domain.Product;
import com.distribuidora.agua.catalog.infrastructure.ProductRepository;
import com.distribuidora.agua.orders.domain.Modality;
import com.distribuidora.agua.orders.domain.Order;
import com.distribuidora.agua.orders.domain.OrderItem;
import com.distribuidora.agua.orders.domain.OrderStatus;
import com.distribuidora.agua.orders.infrastructure.OrderRepository;
import com.distribuidora.agua.platform.api.ResourceNotFoundException;
import com.distribuidora.agua.platform.feature.FeatureFlagKey;
import com.distribuidora.agua.platform.feature.FeatureFlagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Service managing orders lifecycle, state machine transitions, stock debit and atomic reversals.
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    public static final BigDecimal DEFAULT_DELIVERY_FEE = new BigDecimal("5.00");
    public static final BigDecimal PROMO_FREE_DELIVERY_THRESHOLD = new BigDecimal("50.00");
    private static final DateTimeFormatter CODE_DATE_FORMAT = DateTimeFormatter
            .ofPattern("yyyyMMdd")
            .withZone(ZoneId.systemDefault());

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final StockMovementService stockMovementService;
    private final FeatureFlagService featureFlagService;
    private final com.distribuidora.agua.platform.observability.RedMetricsService redMetricsService;

    public OrderService(
            OrderRepository orderRepository,
            ProductRepository productRepository,
            StockMovementService stockMovementService,
            FeatureFlagService featureFlagService,
            com.distribuidora.agua.platform.observability.RedMetricsService redMetricsService
    ) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.stockMovementService = stockMovementService;
        this.featureFlagService = featureFlagService;
        this.redMetricsService = redMetricsService;
    }

    /**
     * Creates a new sales order with atomic stock deduction and dynamic delivery fee calculation.
     */
    @Transactional
    public Order createOrder(CreateOrderCommand command) {
        long startTime = System.nanoTime();
        try {
            Objects.requireNonNull(command, "CreateOrderCommand cannot be null");
            if (command.items() == null || command.items().isEmpty()) {
                throw new IllegalArgumentException("O pedido deve conter pelo menos um item");
            }

            String orderCode = generateOrderCode();

            Order order = Order.builder()
                    .orderCode(orderCode)
                    .customerName(command.customerName())
                    .customerPhone(command.customerPhone())
                    .modality(command.modality())
                    .paymentMethod(command.paymentMethod())
                    .status(OrderStatus.PENDING)
                    .deliveryFee(BigDecimal.ZERO)
                    .build();

            // Add items and debit stock atomically
            for (OrderItemCommand itemCmd : command.items()) {
                Product product = productRepository.findById(itemCmd.productId())
                        .orElseThrow(() -> new ResourceNotFoundException("Produto nao encontrado com id: " + itemCmd.productId()));

                if (!product.isActive()) {
                    throw new IllegalStateException("Produto inativo nao pode ser vendido: " + product.getSku());
                }

                stockMovementService.debitStock(product, itemCmd.quantity());
                order.addItem(product, itemCmd.quantity(), itemCmd.bottlesReturned());
            }

            order.recalculateTotals();

            // Calculate delivery fee
            BigDecimal deliveryFee = calculateDeliveryFee(order.getModality(), order.getSubtotal());
            Order finalOrder = Order.builder()
                    .id(order.getId())
                    .orderCode(order.getOrderCode())
                    .customerName(order.getCustomerName())
                    .customerPhone(order.getCustomerPhone())
                    .modality(order.getModality())
                    .paymentMethod(order.getPaymentMethod())
                    .status(order.getStatus())
                    .deliveryFee(deliveryFee)
                    .build();

            for (OrderItem item : order.getItems()) {
                finalOrder.addItem(item.getProduct(), item.getQuantity(), item.isBottlesReturned());
            }
            finalOrder.recalculateTotals();

            Order saved = orderRepository.save(finalOrder);
            log.info("Created order [{}] for customer [{}] with total [{}]",
                    saved.getOrderCode(), saved.getCustomerName(), saved.getTotalAmount());

            if (redMetricsService != null) {
                redMetricsService.recordOrderCreated(saved.getStatus().name(), saved.getModality().name());
                redMetricsService.recordOrderCreationDuration(java.time.Duration.ofNanos(System.nanoTime() - startTime));
            }
            return saved;
        } catch (Exception ex) {
            if (redMetricsService != null) {
                redMetricsService.recordOrderError(ex.getClass().getSimpleName());
            }
            throw ex;
        }
    }

    /**
     * Advances or updates the status of an existing order.
     */
    @Transactional
    public Order updateStatus(Long orderId, OrderStatus newStatus) {
        Order order = findById(orderId);
        OrderStatus previous = order.getStatus();
        order.transitionTo(newStatus);
        Order updated = orderRepository.save(order);
        log.info("Order [{}] transitioned status from [{}] to [{}]", order.getOrderCode(), previous, newStatus);
        return updated;
    }

    /**
     * Cancels an order, rolling back and refunding dual stock in the catalog module.
     */
    @Transactional
    public Order cancelOrder(Long orderId, String reason) {
        Order order = findById(orderId);

        // Cancel order state machine
        order.cancel();

        // Atomic reversal of stock for all items
        for (OrderItem item : order.getItems()) {
            stockMovementService.creditStock(item.getProduct(), item.getQuantity());
            log.info("Reverted stock of [{}] units for product [{}] due to order cancellation",
                    item.getQuantity(), item.getProduct().getSku());
        }

        Order cancelled = orderRepository.save(order);
        log.warn("Order [{}] was CANCELLED. Reason: [{}]", order.getOrderCode(), reason);
        return cancelled;
    }

    /**
     * Finds an order by its internal database ID.
     */
    @Transactional(readOnly = true)
    public Order findById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido nao encontrado com id: " + orderId));
    }

    /**
     * Finds an order by its unique public business code.
     */
    @Transactional(readOnly = true)
    public Order findByOrderCode(String orderCode) {
        return orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido nao encontrado com codigo: " + orderCode));
    }

    /**
     * Lists orders with pagination and optional filtering by status and customer phone.
     */
    @Transactional(readOnly = true)
    public Page<Order> findOrders(OrderStatus status, String customerPhone, Pageable pageable) {
        if (status != null && customerPhone != null && !customerPhone.isBlank()) {
            return orderRepository.findByStatusAndCustomerPhone(status, customerPhone.trim(), pageable);
        }
        if (status != null) {
            return orderRepository.findByStatus(status, pageable);
        }
        if (customerPhone != null && !customerPhone.isBlank()) {
            return orderRepository.findByCustomerPhone(customerPhone.trim(), pageable);
        }
        return orderRepository.findAll(pageable);
    }

    private BigDecimal calculateDeliveryFee(Modality modality, BigDecimal subtotal) {
        if (modality == Modality.PICKUP) {
            return BigDecimal.ZERO;
        }

        boolean isFreeDeliveryPromo = featureFlagService.isEnabled(FeatureFlagKey.FREE_DELIVERY_PROMO);
        if (isFreeDeliveryPromo && subtotal.compareTo(PROMO_FREE_DELIVERY_THRESHOLD) >= 0) {
            log.info("Applied Free Delivery promo for subtotal [{}]", subtotal);
            return BigDecimal.ZERO;
        }

        return DEFAULT_DELIVERY_FEE;
    }

    private String generateOrderCode() {
        String datePart = CODE_DATE_FORMAT.format(Instant.now());
        int randomPart = ThreadLocalRandom.current().nextInt(100000, 999999);
        return String.format("ORD-%s-%06d", datePart, randomPart);
    }
}
