package com.distribuidora.agua.orders.api;

import com.distribuidora.agua.orders.domain.Modality;
import com.distribuidora.agua.orders.domain.Order;
import com.distribuidora.agua.orders.domain.OrderStatus;
import com.distribuidora.agua.orders.domain.PaymentMethod;
import com.distribuidora.agua.platform.fiscal.FiscalReceipt;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Public response DTO for order details, items and optional fiscal receipt.
 */
public record OrderResponse(
        Long id,
        String orderCode,
        String customerName,
        String customerPhone,
        Modality modality,
        OrderStatus status,
        PaymentMethod paymentMethod,
        BigDecimal subtotal,
        BigDecimal deliveryFee,
        BigDecimal totalAmount,
        Instant createdAt,
        List<OrderItemResponse> items,
        FiscalReceiptResponse fiscalReceipt
) {
    public static OrderResponse fromDomain(Order order) {
        return fromDomain(order, null);
    }

    public static OrderResponse fromDomain(Order order, FiscalReceipt fiscalReceipt) {
        List<OrderItemResponse> itemResponses = order.getItems() != null
                ? order.getItems().stream().map(OrderItemResponse::fromDomain).toList()
                : List.of();

        return new OrderResponse(
                order.getId(),
                order.getOrderCode(),
                order.getCustomerName(),
                order.getCustomerPhone(),
                order.getModality(),
                order.getStatus(),
                order.getPaymentMethod(),
                order.getSubtotal(),
                order.getDeliveryFee(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                itemResponses,
                FiscalReceiptResponse.fromDomain(fiscalReceipt)
        );
    }
}
