package com.distribuidora.agua;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class GerenciadorVendasAguaApplicationTests {

    @Test
    @DisplayName("Deve carregar o contexto da aplicacao com sucesso")
    void contextLoads() {
        // Se o contexto do Spring Boot inicializar sem excecoes, o teste e aprovado
        assertThat(true).isTrue();
    }
}
