package com.distribuidora.agua.catalog.domain;

import org.springframework.stereotype.Component;

@Component
public class AguaCompletaStockStrategy implements StockMovementStrategy {

    @Override
    public boolean supports(TipoProduto tipoProduto) {
        return TipoProduto.AGUA_COMPLETA == tipoProduto;
    }

    @Override
    public void debitStock(Product product, int quantity) {
        if (product.getEstoqueCheios() < quantity) {
            throw new InsufficientStockException("Saldo insuficiente de unidades cheias para o produto: " + product.getSku());
        }
        product.setEstoqueCheios(product.getEstoqueCheios() - quantity);
    }

    @Override
    public void creditStock(Product product, int quantity) {
        product.setEstoqueCheios(product.getEstoqueCheios() + quantity);
    }
}
