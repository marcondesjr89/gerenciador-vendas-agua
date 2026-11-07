package com.distribuidora.agua.orders.api;

import com.distribuidora.agua.platform.fiscal.FiscalReceipt;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response DTO representing the preview of a fiscal receipt emitted for an order.
 */
public record FiscalReceiptResponse(
        String receiptNumber,
        String storeName,
        String storeCnpj,
        String storeAddress,
        String orderCode,
        Instant issuedAt,
        BigDecimal subtotal,
        BigDecimal deliveryFee,
        BigDecimal discount,
        BigDecimal totalAmount,
        String paymentMethod,
        String verificationCode,
        String status,
        String formattedText
) {
    public static FiscalReceiptResponse fromDomain(FiscalReceipt receipt) {
        if (receipt == null) {
            return null;
        }
        return new FiscalReceiptResponse(
                receipt.receiptNumber(),
                receipt.storeName(),
                receipt.storeCnpj(),
                receipt.storeAddress(),
                receipt.orderCode(),
                receipt.issuedAt(),
                receipt.subtotal(),
                receipt.deliveryFee(),
                receipt.discount(),
                receipt.totalAmount(),
                receipt.paymentMethod(),
                receipt.verificationCode(),
                receipt.status(),
                receipt.formattedText()
        );
    }
}
