package com.distribuidora.agua.catalog.api;

import com.distribuidora.agua.catalog.domain.Product;
import com.distribuidora.agua.catalog.domain.ProductType;

import java.io.Serializable;
import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String sku,
        String name,
        BigDecimal capacityLiters,
        boolean returnable,
        ProductType productType,
        BigDecimal unitPrice,
        int stockFull,
        int stockEmpty,
        boolean active,
        Long version
) implements Serializable {

    public static ProductResponse fromEntity(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getCapacityLiters(),
                product.isReturnable(),
                product.getProductType(),
                product.getUnitPrice(),
                product.getStockFull(),
                product.getStockEmpty(),
                product.isActive(),
                product.getVersion()
        );
    }
}
