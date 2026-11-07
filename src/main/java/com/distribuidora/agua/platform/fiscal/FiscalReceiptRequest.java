package com.distribuidora.agua.platform.fiscal;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Domain request sent to FiscalReceiptPort when an order reaches confirmed status.
 */
public record FiscalReceiptRequest(
        UUID orderId,
        String orderCode,
        String customerName,
        String customerPhone,
        List<FiscalReceiptItem> items,
        BigDecimal subtotal,
        BigDecimal deliveryFee,
        BigDecimal discount,
        BigDecimal totalAmount,
        String paymentMethod
) {
}
