package com.distribuidora.agua.catalog.infrastructure;

import com.distribuidora.agua.catalog.domain.Product;
import com.distribuidora.agua.catalog.domain.TipoProduto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("default")
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Test
    @DisplayName("Deve salvar e recuperar produto por SKU")
    void deveSalvarERecuperarPorSku() {
        Product product = Product.builder()
                .sku("GALAO-20L-COMPLETO")
                .nome("Galao 20L Completo (Agua + Vasilhame)")
                .capacidadeLitros(new BigDecimal("20.00"))
                .retornavel(true)
                .tipoProduto(TipoProduto.AGUA_COMPLETA)
                .precoVenda(new BigDecimal("35.00"))
                .estoqueCheios(40)
                .estoqueVazios(10)
                .ativo(true)
                .build();

        Product saved = productRepository.save(product);
        assertThat(saved.getId()).isNotNull();

        Optional<Product> found = productRepository.findBySku("GALAO-20L-COMPLETO");
        assertThat(found).isPresent();
        assertThat(found.get().getNome()).isEqualTo("Galao 20L Completo (Agua + Vasilhame)");
        assertThat(found.get().getTipoProduto()).isEqualTo(TipoProduto.AGUA_COMPLETA);
        assertThat(found.get().getVersao()).isNotNull();
    }

    @Test
    @DisplayName("Deve listar apenas produtos ativos")
    void deveListarApenasAtivos() {
        Product p1 = Product.builder()
                .sku("AGUA-500ML")
                .nome("Garrafa 500ml")
                .capacidadeLitros(new BigDecimal("0.50"))
                .retornavel(false)
                .tipoProduto(TipoProduto.DESCARTAVEL)
                .precoVenda(new BigDecimal("2.50"))
                .estoqueCheios(100)
                .estoqueVazios(0)
                .ativo(true)
                .build();

        Product p2 = Product.builder()
                .sku("GALAO-10L-INATIVO")
                .nome("Galao 10L Fora de Linha")
                .capacidadeLitros(new BigDecimal("10.00"))
                .retornavel(true)
                .tipoProduto(TipoProduto.REFIL)
                .precoVenda(new BigDecimal("12.00"))
                .estoqueCheios(0)
                .estoqueVazios(0)
                .ativo(false)
                .build();

        productRepository.save(p1);
        productRepository.save(p2);

        List<Product> ativos = productRepository.findByAtivoTrue();
        assertThat(ativos).extracting(Product::getSku).contains("AGUA-500ML");
        assertThat(ativos).extracting(Product::getSku).doesNotContain("GALAO-10L-INATIVO");
    }
}
