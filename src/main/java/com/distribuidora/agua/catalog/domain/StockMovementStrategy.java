package com.distribuidora.agua.catalog.domain;

public interface StockMovementStrategy {

    boolean supports(TipoProduto tipoProduto);

    void debitStock(Product product, int quantity);

    void creditStock(Product product, int quantity);
}
