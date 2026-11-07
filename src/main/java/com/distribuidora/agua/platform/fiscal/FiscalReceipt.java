package com.distribuidora.agua.platform.fiscal;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Structured fiscal receipt / preview payload generated for orders.
 */
public record FiscalReceipt(
        String receiptNumber,
        String storeName,
        String storeCnpj,
        String storeAddress,
        UUID orderId,
        String orderCode,
        Instant issuedAt,
        List<FiscalReceiptItem> items,
        BigDecimal subtotal,
        BigDecimal deliveryFee,
        BigDecimal discount,
        BigDecimal totalAmount,
        String paymentMethod,
        String verificationCode,
        String status,
        String formattedText
) {
}
