package com.distribuidora.agua.orders.application;

import com.distribuidora.agua.catalog.application.StockMovementService;
import com.distribuidora.agua.catalog.domain.Product;
import com.distribuidora.agua.catalog.domain.ProductType;
import com.distribuidora.agua.catalog.infrastructure.ProductRepository;
import com.distribuidora.agua.orders.domain.Modality;
import com.distribuidora.agua.orders.domain.Order;
import com.distribuidora.agua.orders.domain.OrderItem;
import com.distribuidora.agua.orders.domain.OrderStatus;
import com.distribuidora.agua.orders.domain.PaymentMethod;
import com.distribuidora.agua.orders.infrastructure.OrderRepository;
import com.distribuidora.agua.platform.api.ResourceNotFoundException;
import com.distribuidora.agua.platform.feature.FeatureFlagKey;
import com.distribuidora.agua.platform.feature.FeatureFlagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
@DisplayName("OrderService Unit Tests")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockMovementService stockMovementService;

    @Mock
    private FeatureFlagService featureFlagService;

    @Mock
    private com.distribuidora.agua.platform.observability.RedMetricsService redMetricsService;

    @InjectMocks
    private OrderService orderService;

    private Product product1;
    private Product product2;

    @BeforeEach
    void setUp() {
        product1 = Product.builder()
                .id(1L)
                .sku("GAL-20L-COMP")
                .name("Galao 20L Completo")
                .productType(ProductType.FULL_WATER)
                .unitPrice(new BigDecimal("30.00"))
                .capacityLiters(new BigDecimal("20.00"))
                .stockFull(50)
                .stockEmpty(10)
                .returnable(true)
                .active(true)
                .build();

        product2 = Product.builder()
                .id(2L)
                .sku("GAR-500ML")
                .name("Garrafa 500ml")
                .productType(ProductType.DISPOSABLE)
                .unitPrice(new BigDecimal("5.00"))
                .capacityLiters(new BigDecimal("0.50"))
                .stockFull(100)
                .stockEmpty(0)
                .returnable(false)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Should successfully create delivery order, debit stock and apply default delivery fee")
    void shouldCreateDeliveryOrderAndDebitStock() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
        when(productRepository.findById(2L)).thenReturn(Optional.of(product2));
        when(featureFlagService.isEnabled(FeatureFlagKey.FREE_DELIVERY_PROMO)).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateOrderCommand command = new CreateOrderCommand(
                "Carlos Ferreira",
                "11988889999",
                Modality.DELIVERY,
                PaymentMethod.PIX,
                List.of(
                        new OrderItemCommand(1L, 2, false),
                        new OrderItemCommand(2L, 4, false)
                )
        );

        Order created = orderService.createOrder(command);

        assertThat(created).isNotNull();
        assertThat(created.getCustomerName()).isEqualTo("Carlos Ferreira");
        assertThat(created.getModality()).isEqualTo(Modality.DELIVERY);
        assertThat(created.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(created.getItems()).hasSize(2);
        assertThat(created.getSubtotal()).isEqualByComparingTo("80.00"); // (2*30) + (4*5)
        assertThat(created.getDeliveryFee()).isEqualByComparingTo("5.00");
        assertThat(created.getTotalAmount()).isEqualByComparingTo("85.00");

        // Verify stock debit for both products
        verify(stockMovementService).debitStock(product1, 2);
        verify(stockMovementService).debitStock(product2, 4);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("Should apply free delivery when feature flag is active and subtotal qualifies")
    void shouldApplyFreeDeliveryWhenPromoFlagActive() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
        when(featureFlagService.isEnabled(FeatureFlagKey.FREE_DELIVERY_PROMO)).thenReturn(true);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateOrderCommand command = new CreateOrderCommand(
                "Renata Souza",
                "11977778888",
                Modality.DELIVERY,
                PaymentMethod.CREDIT_CARD,
                List.of(new OrderItemCommand(1L, 2, false)) // 2 * 30.00 = 60.00 (>= 50.00)
        );

        Order created = orderService.createOrder(command);

        assertThat(created.getSubtotal()).isEqualByComparingTo("60.00");
        assertThat(created.getDeliveryFee()).isEqualByComparingTo("0.00");
        assertThat(created.getTotalAmount()).isEqualByComparingTo("60.00");
    }

    @Test
    @DisplayName("Should set delivery fee to zero for pickup modality regardless of feature flag")
    void shouldSetZeroDeliveryFeeForPickup() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateOrderCommand command = new CreateOrderCommand(
                "Balcao Cliente",
                "11900001111",
                Modality.PICKUP,
                PaymentMethod.CASH,
                List.of(new OrderItemCommand(1L, 1, false))
        );

        Order created = orderService.createOrder(command);

        assertThat(created.getDeliveryFee()).isEqualByComparingTo("0.00");
        assertThat(created.getTotalAmount()).isEqualByComparingTo("30.00");
    }

    @Test
    @DisplayName("Should cancel order and automatically credit back stock for all items")
    void shouldCancelOrderAndCreditBackStock() {
        Order order = Order.builder()
                .id(10L)
                .orderCode("ORD-10")
                .customerName("Joao")
                .customerPhone("11999990000")
                .modality(Modality.DELIVERY)
                .paymentMethod(PaymentMethod.PIX)
                .deliveryFee(new BigDecimal("5.00"))
                .build();
        order.addItem(product1, 2, false);
        order.addItem(product2, 1, false);
        order.recalculateTotals();
        order.transitionTo(OrderStatus.CONFIRMED);

        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order cancelled = orderService.cancelOrder(10L, "Cliente desistiu");

        assertThat(cancelled.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        // Verify credit back for all items in order
        verify(stockMovementService).creditStock(product1, 2);
        verify(stockMovementService).creditStock(product2, 1);
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("Should reject cancelOrder when order is already DELIVERED")
    void shouldRejectCancelWhenAlreadyDelivered() {
        Order order = Order.builder()
                .id(11L)
                .orderCode("ORD-11")
                .customerName("Maria")
                .customerPhone("11988881111")
                .modality(Modality.PICKUP)
                .paymentMethod(PaymentMethod.CASH)
                .build();
        order.addItem(product1, 1, false);
        order.recalculateTotals();
        order.transitionTo(OrderStatus.CONFIRMED);
        order.transitionTo(OrderStatus.DELIVERED);

        when(orderRepository.findById(11L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(11L, "Tarde demais"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DELIVERED");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when order does not exist")
    void shouldThrowExceptionWhenOrderNotFound() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.findById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }
}
