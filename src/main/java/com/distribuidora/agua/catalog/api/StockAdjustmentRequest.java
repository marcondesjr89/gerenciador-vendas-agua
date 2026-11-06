package com.distribuidora.agua.catalog.api;

import jakarta.validation.constraints.Min;

public record StockAdjustmentRequest(
        @Min(value = 0, message = "Stock full cannot be negative")
        int stockFull,

        @Min(value = 0, message = "Stock empty cannot be negative")
        int stockEmpty
) {}
