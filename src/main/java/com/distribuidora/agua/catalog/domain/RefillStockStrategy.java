package com.distribuidora.agua.catalog.domain;

import org.springframework.stereotype.Component;

@Component
public class RefillStockStrategy implements StockMovementStrategy {

    @Override
    public boolean supports(ProductType productType) {
        return ProductType.REFILL == productType;
    }

    @Override
    public void debitStock(Product product, int quantity) {
        if (product.getStockFull() < quantity) {
            throw new InsufficientStockException("Insufficient full units stock for product: " + product.getSku());
        }
        product.setStockFull(product.getStockFull() - quantity);
        product.setStockEmpty(product.getStockEmpty() + quantity);
    }

    @Override
    public void creditStock(Product product, int quantity) {
        product.setStockFull(product.getStockFull() + quantity);
        product.setStockEmpty(Math.max(0, product.getStockEmpty() - quantity));
    }
}
