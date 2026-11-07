package com.distribuidora.agua.platform.fiscal;

import java.math.BigDecimal;

/**
 * Line item represented on a fiscal receipt preview.
 */
public record FiscalReceiptItem(
        String sku,
        String description,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice
) {
}
