package com.distribuidora.agua.catalog.domain;

import org.springframework.stereotype.Component;

@Component
public class VasilhameVazioStockStrategy implements StockMovementStrategy {

    @Override
    public boolean supports(TipoProduto tipoProduto) {
        return TipoProduto.VASILHAME_VAZIO == tipoProduto;
    }

    @Override
    public void debitStock(Product product, int quantity) {
        if (product.getEstoqueVazios() < quantity) {
            throw new InsufficientStockException("Saldo insuficiente de vasilhames vazios para o produto: " + product.getSku());
        }
        product.setEstoqueVazios(product.getEstoqueVazios() - quantity);
    }

    @Override
    public void creditStock(Product product, int quantity) {
        product.setEstoqueVazios(product.getEstoqueVazios() + quantity);
    }
}
