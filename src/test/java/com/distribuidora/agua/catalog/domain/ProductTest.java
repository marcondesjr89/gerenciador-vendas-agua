package com.distribuidora.agua.catalog.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    @Test
    @DisplayName("Deve criar produto valido com todos os campos obrigatorios")
    void deveCriarProdutoValido() {
        Product product = Product.builder()
                .sku("GALAO-20L")
                .nome("Galao de Agua 20L")
                .capacidadeLitros(new BigDecimal("20.00"))
                .retornavel(true)
                .tipoProduto(TipoProduto.AGUA_COMPLETA)
                .precoVenda(new BigDecimal("30.00"))
                .estoqueCheios(50)
                .estoqueVazios(20)
                .ativo(true)
                .build();

        assertThat(product.getSku()).isEqualTo("GALAO-20L");
        assertThat(product.getNome()).isEqualTo("Galao de Agua 20L");
        assertThat(product.getCapacidadeLitros()).isEqualByComparingTo("20.00");
        assertThat(product.isRetornavel()).isTrue();
        assertThat(product.getTipoProduto()).isEqualTo(TipoProduto.AGUA_COMPLETA);
        assertThat(product.getPrecoVenda()).isEqualByComparingTo("30.00");
        assertThat(product.getEstoqueCheios()).isEqualTo(50);
        assertThat(product.getEstoqueVazios()).isEqualTo(20);
        assertThat(product.isAtivo()).isTrue();
    }

    @Test
    @DisplayName("Deve rejeitar criacao de produto com campos nulos obrigatorios")
    void deveRejeitarCamposNulos() {
        Product.Builder baseBuilder = Product.builder()
                .sku("SKU-TESTE")
                .nome("Produto Teste")
                .capacidadeLitros(new BigDecimal("1.00"))
                .tipoProduto(TipoProduto.DESCARTAVEL)
                .precoVenda(new BigDecimal("5.00"));

        assertThatThrownBy(() -> baseBuilder.sku(null).build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("SKU nao pode ser nulo");

        assertThatThrownBy(() -> baseBuilder.sku("SKU-TESTE").nome(null).build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Nome nao pode ser nulo");

        assertThatThrownBy(() -> baseBuilder.nome("Nome").capacidadeLitros(null).build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Capacidade em litros nao pode ser nula");

        assertThatThrownBy(() -> baseBuilder.capacidadeLitros(BigDecimal.ONE).tipoProduto(null).build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Tipo do produto nao pode ser nulo");

        assertThatThrownBy(() -> baseBuilder.tipoProduto(TipoProduto.DESCARTAVEL).precoVenda(null).build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Preco de venda nao pode ser nulo");
    }

    @Test
    @DisplayName("Deve rejeitar preco de venda menor ou igual a zero")
    void deveRejeitarPrecoInvalido() {
        Product.Builder baseBuilder = Product.builder()
                .sku("SKU-TESTE")
                .nome("Produto Teste")
                .capacidadeLitros(new BigDecimal("1.00"))
                .tipoProduto(TipoProduto.DESCARTAVEL);

        assertThatThrownBy(() -> baseBuilder.precoVenda(BigDecimal.ZERO).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Preco de venda deve ser maior que zero");

        assertThatThrownBy(() -> baseBuilder.precoVenda(new BigDecimal("-10.00")).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Preco de venda deve ser maior que zero");
    }

    @Test
    @DisplayName("Deve rejeitar capacidade em litros menor ou igual a zero")
    void deveRejeitarCapacidadeInvalida() {
        Product.Builder baseBuilder = Product.builder()
                .sku("SKU-TESTE")
                .nome("Produto Teste")
                .tipoProduto(TipoProduto.DESCARTAVEL)
                .precoVenda(new BigDecimal("5.00"));

        assertThatThrownBy(() -> baseBuilder.capacidadeLitros(BigDecimal.ZERO).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Capacidade em litros deve ser maior que zero");

        assertThatThrownBy(() -> baseBuilder.capacidadeLitros(new BigDecimal("-1.50")).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Capacidade em litros deve ser maior que zero");
    }

    @Test
    @DisplayName("Deve rejeitar saldo inicial de estoque negativo")
    void deveRejeitarSaldoNegativo() {
        Product.Builder baseBuilder = Product.builder()
                .sku("SKU-TESTE")
                .nome("Produto Teste")
                .capacidadeLitros(new BigDecimal("1.00"))
                .tipoProduto(TipoProduto.DESCARTAVEL)
                .precoVenda(new BigDecimal("5.00"));

        assertThatThrownBy(() -> baseBuilder.estoqueCheios(-1).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Saldos de estoque nao podem ser negativos");

        assertThatThrownBy(() -> baseBuilder.estoqueCheios(0).estoqueVazios(-5).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Saldos de estoque nao podem ser negativos");
    }
}
