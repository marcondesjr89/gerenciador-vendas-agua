package com.distribuidora.agua.catalog.domain;

import org.springframework.stereotype.Component;

@Component
public class EmptyBottleStockStrategy implements StockMovementStrategy {

    @Override
    public boolean supports(ProductType productType) {
        return ProductType.EMPTY_BOTTLE == productType;
    }

    @Override
    public void debitStock(Product product, int quantity) {
        if (product.getStockEmpty() < quantity) {
            throw new InsufficientStockException("Insufficient empty bottles stock for product: " + product.getSku());
        }
        product.setStockEmpty(product.getStockEmpty() - quantity);
    }

    @Override
    public void creditStock(Product product, int quantity) {
        product.setStockEmpty(product.getStockEmpty() + quantity);
    }
}
