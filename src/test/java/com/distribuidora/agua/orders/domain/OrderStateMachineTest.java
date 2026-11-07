package com.distribuidora.agua.orders.domain;

import com.distribuidora.agua.catalog.domain.Product;
import com.distribuidora.agua.catalog.domain.ProductType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Order and State Machine Domain Unit Tests")
class OrderStateMachineTest {

    private Product fullWaterProduct;

    @BeforeEach
    void setUp() {
        fullWaterProduct = Product.builder()
                .sku("GAL-20L-COMP")
                .name("Galao 20L Completo")
                .productType(ProductType.FULL_WATER)
                .unitPrice(new BigDecimal("30.00"))
                .capacityLiters(new BigDecimal("20.00"))
                .stockFull(50)
                .stockEmpty(20)
                .returnable(true)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Should successfully transition through delivery order lifecycle: PENDING -> CONFIRMED -> IN_TRANSIT -> DELIVERED")
    void shouldFollowValidDeliveryLifecycle() {
        Order order = Order.builder()
                .orderCode("ORD-202611-0001")
                .customerName("Ana Paula")
                .customerPhone("11988887777")
                .modality(Modality.DELIVERY)
                .paymentMethod(PaymentMethod.PIX)
                .deliveryFee(new BigDecimal("5.00"))
                .build();

        order.addItem(fullWaterProduct, 2, false);
        order.recalculateTotals();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getSubtotal()).isEqualByComparingTo("60.00");
        assertThat(order.getTotalAmount()).isEqualByComparingTo("65.00");

        // Transition 1: Confirm
        order.transitionTo(OrderStatus.CONFIRMED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);

        // Transition 2: Dispatch for delivery
        order.transitionTo(OrderStatus.IN_TRANSIT);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.IN_TRANSIT);

        // Transition 3: Deliver
        order.transitionTo(OrderStatus.DELIVERED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    @DisplayName("Should allow pickup order to transition directly from CONFIRMED to DELIVERED")
    void shouldAllowPickupDirectDelivery() {
        Order order = Order.builder()
                .orderCode("ORD-202611-0002")
                .customerName("Marcos Silva")
                .customerPhone("11977776666")
                .modality(Modality.PICKUP)
                .paymentMethod(PaymentMethod.CASH)
                .deliveryFee(BigDecimal.ZERO)
                .build();

        order.addItem(fullWaterProduct, 1, false);
        order.recalculateTotals();

        order.transitionTo(OrderStatus.CONFIRMED);
        order.transitionTo(OrderStatus.DELIVERED);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    @DisplayName("Should not allow pickup order to transition to IN_TRANSIT")
    void shouldRejectInTransitForPickupModality() {
        Order order = Order.builder()
                .orderCode("ORD-202611-0003")
                .customerName("Balcao")
                .customerPhone("11900000000")
                .modality(Modality.PICKUP)
                .paymentMethod(PaymentMethod.DEBIT_CARD)
                .deliveryFee(BigDecimal.ZERO)
                .build();

        order.addItem(fullWaterProduct, 1, false);
        order.transitionTo(OrderStatus.CONFIRMED);

        assertThatThrownBy(() -> order.transitionTo(OrderStatus.IN_TRANSIT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PICKUP");
    }

    @Test
    @DisplayName("Should allow cancellation from PENDING, CONFIRMED, or IN_TRANSIT")
    void shouldAllowCancellationFromNonTerminalStates() {
        Order order1 = Order.builder().orderCode("ORD-1").customerName("C1").customerPhone("P1")
                .modality(Modality.DELIVERY).paymentMethod(PaymentMethod.PIX).build();
        order1.transitionTo(OrderStatus.CANCELLED);
        assertThat(order1.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        Order order2 = Order.builder().orderCode("ORD-2").customerName("C2").customerPhone("P2")
                .modality(Modality.DELIVERY).paymentMethod(PaymentMethod.PIX).build();
        order2.transitionTo(OrderStatus.CONFIRMED);
        order2.transitionTo(OrderStatus.CANCELLED);
        assertThat(order2.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        Order order3 = Order.builder().orderCode("ORD-3").customerName("C3").customerPhone("P3")
                .modality(Modality.DELIVERY).paymentMethod(PaymentMethod.PIX).build();
        order3.transitionTo(OrderStatus.CONFIRMED);
        order3.transitionTo(OrderStatus.IN_TRANSIT);
        order3.transitionTo(OrderStatus.CANCELLED);
        assertThat(order3.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("Should reject cancellation once order is DELIVERED")
    void shouldRejectCancellationWhenDelivered() {
        Order order = Order.builder()
                .orderCode("ORD-202611-0004")
                .customerName("Carla")
                .customerPhone("11911112222")
                .modality(Modality.PICKUP)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .build();

        order.addItem(fullWaterProduct, 1, false);
        order.transitionTo(OrderStatus.CONFIRMED);
        order.transitionTo(OrderStatus.DELIVERED);

        assertThatThrownBy(() -> order.transitionTo(OrderStatus.CANCELLED))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DELIVERED");
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"PENDING", "CONFIRMED", "IN_TRANSIT", "DELIVERED"})
    @DisplayName("Should reject any transition once order is CANCELLED")
    void shouldRejectAnyTransitionWhenAlreadyCancelled(OrderStatus targetStatus) {
        Order order = Order.builder()
                .orderCode("ORD-202611-0005")
                .customerName("Lucas")
                .customerPhone("11933334444")
                .modality(Modality.DELIVERY)
                .paymentMethod(PaymentMethod.PIX)
                .build();

        order.transitionTo(OrderStatus.CANCELLED);

        assertThatThrownBy(() -> order.transitionTo(targetStatus))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CANCELLED");
    }
}
