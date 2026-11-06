package com.distribuidora.agua.catalog.application;

import com.distribuidora.agua.catalog.domain.AguaCompletaStockStrategy;
import com.distribuidora.agua.catalog.domain.DescartavelStockStrategy;
import com.distribuidora.agua.catalog.domain.Product;
import com.distribuidora.agua.catalog.domain.RefilStockStrategy;
import com.distribuidora.agua.catalog.domain.StockMovementStrategy;
import com.distribuidora.agua.catalog.domain.TipoProduto;
import com.distribuidora.agua.catalog.domain.VasilhameVazioStockStrategy;
import com.distribuidora.agua.catalog.infrastructure.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StockMovementServiceTest {

    @Mock
    private ProductRepository productRepository;

    private StockMovementService stockMovementService;
    private Product product;

    @BeforeEach
    void setUp() {
        List<StockMovementStrategy> strategies = List.of(
                new AguaCompletaStockStrategy(),
                new RefilStockStrategy(),
                new VasilhameVazioStockStrategy(),
                new DescartavelStockStrategy()
        );
        stockMovementService = new StockMovementService(strategies, productRepository);

        product = Product.builder()
                .id(10L)
                .sku("REFIL-20L")
                .nome("Refil Galao 20L")
                .capacidadeLitros(new BigDecimal("20.00"))
                .retornavel(true)
                .tipoProduto(TipoProduto.REFIL)
                .precoVenda(new BigDecimal("15.00"))
                .estoqueCheios(20)
                .estoqueVazios(10)
                .ativo(true)
                .build();
    }

    @Test
    @DisplayName("Deve movimentar baixa com sucesso delegando para a strategy correta")
    void deveMovimentarBaixa() {
        stockMovementService.movimentarBaixa(product, 5);

        assertThat(product.getEstoqueCheios()).isEqualTo(15);
        assertThat(product.getEstoqueVazios()).isEqualTo(15);
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("Deve movimentar estorno com sucesso delegando para a strategy correta")
    void deveMovimentarEstorno() {
        stockMovementService.movimentarEstorno(product, 2);

        assertThat(product.getEstoqueCheios()).isEqualTo(22);
        assertThat(product.getEstoqueVazios()).isEqualTo(8);
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("Deve falhar ao tentar movimentar quantidade menor ou igual a zero")
    void deveFalharQuantidadeInvalida() {
        assertThatThrownBy(() -> stockMovementService.movimentarBaixa(product, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("A quantidade deve ser maior que zero");

        assertThatThrownBy(() -> stockMovementService.movimentarEstorno(product, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("A quantidade deve ser maior que zero");
    }

    @Test
    @DisplayName("Deve ajustar estoque diretamente com valores validos")
    void deveAjustarEstoqueDiretamente() {
        stockMovementService.ajustarEstoque(product, 50, 30);

        assertThat(product.getEstoqueCheios()).isEqualTo(50);
        assertThat(product.getEstoqueVazios()).isEqualTo(30);
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("Deve permitir ajuste de estoque para limite inferior zero")
    void devePermitirAjusteParaZero() {
        stockMovementService.ajustarEstoque(product, 0, 0);

        assertThat(product.getEstoqueCheios()).isZero();
        assertThat(product.getEstoqueVazios()).isZero();
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("Deve falhar ao ajustar estoque com valores negativos")
    void deveFalharAjusteEstoqueNegativo() {
        assertThatThrownBy(() -> stockMovementService.ajustarEstoque(product, -5, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Os saldos de estoque nao podem ser negativos");

        assertThatThrownBy(() -> stockMovementService.ajustarEstoque(product, 10, -2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Os saldos de estoque nao podem ser negativos");
    }

    @Test
    @DisplayName("Deve lancar IllegalStateException se nao houver strategy para o TipoProduto")
    void deveFalharQuandoStrategyNaoEncontrada() {
        StockMovementService servicoSemStrategies = new StockMovementService(Collections.emptyList(), productRepository);

        assertThatThrownBy(() -> servicoSemStrategies.movimentarBaixa(product, 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Nenhuma estrategia de estoque encontrada");
    }
}
