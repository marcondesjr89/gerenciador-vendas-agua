package com.distribuidora.agua.catalog.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockMovementStrategyTest {

    private Product product;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .id(1L)
                .sku("GALAO-20L")
                .nome("Galao de Agua 20L")
                .capacidadeLitros(new BigDecimal("20.00"))
                .retornavel(true)
                .tipoProduto(TipoProduto.AGUA_COMPLETA)
                .precoVenda(new BigDecimal("30.00"))
                .estoqueCheios(10)
                .estoqueVazios(5)
                .ativo(true)
                .build();
    }

    @Nested
    @DisplayName("Testes para AguaCompletaStockStrategy")
    class AguaCompletaTests {
        private final StockMovementStrategy strategy = new AguaCompletaStockStrategy();

        @Test
        @DisplayName("Deve suportar apenas TipoProduto.AGUA_COMPLETA")
        void deveSuportarTipoCorreto() {
            assertThat(strategy.supports(TipoProduto.AGUA_COMPLETA)).isTrue();
            assertThat(strategy.supports(TipoProduto.REFIL)).isFalse();
        }

        @Test
        @DisplayName("Deve debitar apenas estoque de cheios")
        void deveDebitarCheios() {
            strategy.debitStock(product, 3);
            assertThat(product.getEstoqueCheios()).isEqualTo(7);
            assertThat(product.getEstoqueVazios()).isEqualTo(5);
        }

        @Test
        @DisplayName("Deve zerar estoque com sucesso quando quantidade for exatamente igual ao saldo")
        void deveZerarEstoqueNoLimiteExato() {
            strategy.debitStock(product, 10);
            assertThat(product.getEstoqueCheios()).isZero();
        }

        @Test
        @DisplayName("Deve estornar/creditar apenas estoque de cheios")
        void deveCreditarCheios() {
            strategy.creditStock(product, 2);
            assertThat(product.getEstoqueCheios()).isEqualTo(12);
            assertThat(product.getEstoqueVazios()).isEqualTo(5);
        }

        @Test
        @DisplayName("Deve lancar InsufficientStockException se saldo de cheios for insuficiente")
        void deveLancarExcecaoQuandoSaldoInsuficiente() {
            assertThatThrownBy(() -> strategy.debitStock(product, 15))
                    .isInstanceOf(InsufficientStockException.class)
                    .hasMessageContaining("Saldo insuficiente de unidades cheias");
        }

        @Test
        @DisplayName("Deve lancar InsufficientStockException se estoque ja estiver zerado")
        void deveLancarExcecaoQuandoEstoqueJaZerado() {
            product.setEstoqueCheios(0);
            assertThatThrownBy(() -> strategy.debitStock(product, 1))
                    .isInstanceOf(InsufficientStockException.class)
                    .hasMessageContaining("Saldo insuficiente de unidades cheias");
        }
    }

    @Nested
    @DisplayName("Testes para RefilStockStrategy")
    class RefilTests {
        private final StockMovementStrategy strategy = new RefilStockStrategy();

        @Test
        @DisplayName("Deve suportar apenas TipoProduto.REFIL")
        void deveSuportarTipoCorreto() {
            assertThat(strategy.supports(TipoProduto.REFIL)).isTrue();
            assertThat(strategy.supports(TipoProduto.AGUA_COMPLETA)).isFalse();
        }

        @Test
        @DisplayName("Deve debitar cheios e creditar vazios ao vender refil com vasilhame em troca")
        void deveDebitarCheiosECreditarVazios() {
            strategy.debitStock(product, 4);
            assertThat(product.getEstoqueCheios()).isEqualTo(6);
            assertThat(product.getEstoqueVazios()).isEqualTo(9); // 5 + 4
        }

        @Test
        @DisplayName("Deve estornar refil: creditar cheios e debitar vazios")
        void deveEstornarRefil() {
            strategy.creditStock(product, 3);
            assertThat(product.getEstoqueCheios()).isEqualTo(13); // 10 + 3
            assertThat(product.getEstoqueVazios()).isEqualTo(2);  // 5 - 3
        }

        @Test
        @DisplayName("Deve garantir piso zero de vasilhames no estorno se saldo de vazios for menor que o estornado")
        void deveGarantirPisoZeroNoEstorno() {
            product.setEstoqueVazios(1);
            strategy.creditStock(product, 5);
            assertThat(product.getEstoqueCheios()).isEqualTo(15);
            assertThat(product.getEstoqueVazios()).isZero(); // Nao pode ser negativo
        }

        @Test
        @DisplayName("Deve lancar InsufficientStockException se cheios forem insuficientes")
        void deveLancarExcecaoQuandoCheiosInsuficientes() {
            assertThatThrownBy(() -> strategy.debitStock(product, 20))
                    .isInstanceOf(InsufficientStockException.class)
                    .hasMessageContaining("Saldo insuficiente de unidades cheias");
        }
    }

    @Nested
    @DisplayName("Testes para VasilhameVazioStockStrategy")
    class VasilhameVazioTests {
        private final StockMovementStrategy strategy = new VasilhameVazioStockStrategy();

        @Test
        @DisplayName("Deve suportar apenas TipoProduto.VASILHAME_VAZIO")
        void deveSuportarTipoCorreto() {
            assertThat(strategy.supports(TipoProduto.VASILHAME_VAZIO)).isTrue();
            assertThat(strategy.supports(TipoProduto.DESCARTAVEL)).isFalse();
        }

        @Test
        @DisplayName("Deve debitar apenas estoque de vasilhames vazios")
        void deveDebitarVazios() {
            strategy.debitStock(product, 2);
            assertThat(product.getEstoqueCheios()).isEqualTo(10);
            assertThat(product.getEstoqueVazios()).isEqualTo(3);
        }

        @Test
        @DisplayName("Deve zerar estoque de vazios no limite exato")
        void deveZerarEstoqueDeVaziosNoLimiteExato() {
            strategy.debitStock(product, 5);
            assertThat(product.getEstoqueVazios()).isZero();
        }

        @Test
        @DisplayName("Deve creditar apenas estoque de vasilhames vazios no estorno")
        void deveCreditarVazios() {
            strategy.creditStock(product, 4);
            assertThat(product.getEstoqueCheios()).isEqualTo(10);
            assertThat(product.getEstoqueVazios()).isEqualTo(9);
        }

        @Test
        @DisplayName("Deve lancar InsufficientStockException se saldo de vazios for insuficiente")
        void deveLancarExcecaoQuandoVaziosInsuficientes() {
            assertThatThrownBy(() -> strategy.debitStock(product, 10))
                    .isInstanceOf(InsufficientStockException.class)
                    .hasMessageContaining("Saldo insuficiente de vasilhames vazios");
        }
    }

    @Nested
    @DisplayName("Testes para DescartavelStockStrategy")
    class DescartavelTests {
        private final StockMovementStrategy strategy = new DescartavelStockStrategy();

        @Test
        @DisplayName("Deve suportar apenas TipoProduto.DESCARTAVEL")
        void deveSuportarTipoCorreto() {
            assertThat(strategy.supports(TipoProduto.DESCARTAVEL)).isTrue();
            assertThat(strategy.supports(TipoProduto.REFIL)).isFalse();
        }

        @Test
        @DisplayName("Deve debitar cheios sem alterar vazios")
        void deveDebitarCheiosSemAlterarVazios() {
            strategy.debitStock(product, 5);
            assertThat(product.getEstoqueCheios()).isEqualTo(5);
            assertThat(product.getEstoqueVazios()).isEqualTo(5);
        }

        @Test
        @DisplayName("Deve creditar cheios sem alterar vazios no estorno")
        void deveCreditarCheios() {
            strategy.creditStock(product, 5);
            assertThat(product.getEstoqueCheios()).isEqualTo(15);
            assertThat(product.getEstoqueVazios()).isEqualTo(5);
        }

        @Test
        @DisplayName("Deve lancar InsufficientStockException se saldo for insuficiente")
        void deveLancarExcecaoQuandoSaldoInsuficiente() {
            assertThatThrownBy(() -> strategy.debitStock(product, 12))
                    .isInstanceOf(InsufficientStockException.class)
                    .hasMessageContaining("Saldo insuficiente de unidades cheias");
        }
    }
}
