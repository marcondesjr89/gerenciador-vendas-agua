package com.distribuidora.agua.catalog.application;

import com.distribuidora.agua.catalog.domain.Product;
import com.distribuidora.agua.catalog.domain.ProductType;
import com.distribuidora.agua.catalog.domain.StockMovementStrategy;
import com.distribuidora.agua.catalog.infrastructure.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StockMovementService {

    private final List<StockMovementStrategy> strategies;
    private final ProductRepository productRepository;

    public StockMovementService(List<StockMovementStrategy> strategies, ProductRepository productRepository) {
        this.strategies = strategies;
        this.productRepository = productRepository;
    }

    @Transactional
    public void debitStock(Product product, int quantity) {
        validateQuantity(quantity);
        StockMovementStrategy strategy = resolveStrategy(product.getProductType());
        strategy.debitStock(product, quantity);
        productRepository.save(product);
    }

    @Transactional
    public void creditStock(Product product, int quantity) {
        validateQuantity(quantity);
        StockMovementStrategy strategy = resolveStrategy(product.getProductType());
        strategy.creditStock(product, quantity);
        productRepository.save(product);
    }

    @Transactional
    public void adjustStock(Product product, int newStockFull, int newStockEmpty) {
        if (newStockFull < 0 || newStockEmpty < 0) {
            throw new IllegalArgumentException("Stock balances cannot be negative");
        }
        product.setStockFull(newStockFull);
        product.setStockEmpty(newStockEmpty);
        productRepository.save(product);
    }

    private void validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
    }

    private StockMovementStrategy resolveStrategy(ProductType productType) {
        return strategies.stream()
                .filter(strategy -> strategy.supports(productType))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No stock strategy found for product type: " + productType));
    }
}
