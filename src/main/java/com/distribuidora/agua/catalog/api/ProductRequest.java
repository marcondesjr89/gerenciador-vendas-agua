package com.distribuidora.agua.catalog.api;

import com.distribuidora.agua.catalog.domain.ProductType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank(message = "SKU must not be blank")
        @Size(max = 40, message = "SKU must not exceed 40 characters")
        String sku,

        @NotBlank(message = "Name must not be blank")
        @Size(max = 120, message = "Name must not exceed 120 characters")
        String name,

        @NotNull(message = "Capacity in liters must not be null")
        @Positive(message = "Capacity in liters must be greater than zero")
        BigDecimal capacityLiters,

        boolean returnable,

        @NotNull(message = "Product type must not be null")
        ProductType productType,

        @NotNull(message = "Unit price must not be null")
        @Positive(message = "Unit price must be greater than zero")
        BigDecimal unitPrice,

        @Min(value = 0, message = "Stock full cannot be negative")
        int stockFull,

        @Min(value = 0, message = "Stock empty cannot be negative")
        int stockEmpty
) {}
