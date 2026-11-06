package com.distribuidora.agua.catalog.application;

import com.distribuidora.agua.catalog.domain.Product;
import com.distribuidora.agua.catalog.domain.StockMovementStrategy;
import com.distribuidora.agua.catalog.domain.TipoProduto;
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
    public void movimentarBaixa(Product product, int quantity) {
        validarQuantidade(quantity);
        StockMovementStrategy strategy = resolveStrategy(product.getTipoProduto());
        strategy.debitStock(product, quantity);
        productRepository.save(product);
    }

    @Transactional
    public void movimentarEstorno(Product product, int quantity) {
        validarQuantidade(quantity);
        StockMovementStrategy strategy = resolveStrategy(product.getTipoProduto());
        strategy.creditStock(product, quantity);
        productRepository.save(product);
    }

    @Transactional
    public void ajustarEstoque(Product product, int novosCheios, int novosVazios) {
        if (novosCheios < 0 || novosVazios < 0) {
            throw new IllegalArgumentException("Os saldos de estoque nao podem ser negativos");
        }
        product.setEstoqueCheios(novosCheios);
        product.setEstoqueVazios(novosVazios);
        productRepository.save(product);
    }

    private void validarQuantidade(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("A quantidade deve ser maior que zero");
        }
    }

    private StockMovementStrategy resolveStrategy(TipoProduto tipoProduto) {
        return strategies.stream()
                .filter(strategy -> strategy.supports(tipoProduto))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Nenhuma estrategia de estoque encontrada para o tipo: " + tipoProduto));
    }
}
