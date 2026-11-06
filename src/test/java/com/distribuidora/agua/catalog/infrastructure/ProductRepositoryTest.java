package com.distribuidora.agua.catalog.infrastructure;

import com.distribuidora.agua.catalog.domain.Product;
import com.distribuidora.agua.catalog.domain.TipoProduto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("default")
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TestEntityManager entityManager;

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

    @Test
    @DisplayName("Deve detectar conflito de concorrencia otimista com @Version")
    void deveDetectarConflitoConcorrenciaOtimista() {
        Product product = Product.builder()
                .sku("GALAO-20L-LOCK")
                .nome("Galao 20L Concorrencia")
                .capacidadeLitros(new BigDecimal("20.00"))
                .retornavel(true)
                .tipoProduto(TipoProduto.AGUA_COMPLETA)
                .precoVenda(new BigDecimal("30.00"))
                .estoqueCheios(10)
                .estoqueVazios(5)
                .ativo(true)
                .build();

        Product saved = productRepository.saveAndFlush(product);
        Long id = saved.getId();

        // Carrega primeira instancia e a desanexa da sessao
        Product instance1 = productRepository.findById(id).orElseThrow();
        entityManager.detach(instance1);

        // Carrega segunda instancia e realiza atualizacao concorrente
        Product instance2 = productRepository.findById(id).orElseThrow();
        instance2.setEstoqueCheios(15);
        productRepository.saveAndFlush(instance2);

        // Limpa o contexto para forcar merge da instancia 1 desatualizada
        entityManager.clear();

        // A instancia 1 possui versao desatualizada (versao = 0, enquanto no banco ja e 1)
        instance1.setEstoqueCheios(20);
        assertThatThrownBy(() -> productRepository.saveAndFlush(instance1))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }
}
