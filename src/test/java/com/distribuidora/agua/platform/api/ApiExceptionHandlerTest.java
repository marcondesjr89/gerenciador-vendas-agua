package com.distribuidora.agua.platform.api;

import com.distribuidora.agua.platform.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DummyTestController.class)
@Import({SecurityConfig.class, ApiExceptionHandler.class})
class ApiExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Deve retornar ProblemDetail RFC 7807 com 400 Bad Request ao falhar validacao de DTO")
    void deveRetornarProblemDetailAoFalharValidacao() throws Exception {
        String invalidJson = """
                {
                    "nome": "",
                    "valor": -10.0
                }
                """;

        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Dados de requisicao invalidos"))
                .andExpect(jsonPath("$.invalidParams").isArray())
                .andExpect(jsonPath("$.invalidParams[?(@.field == 'nome')]").exists())
                .andExpect(jsonPath("$.invalidParams[?(@.field == 'valor')]").exists());
    }

    @Test
    @DisplayName("Deve retornar ProblemDetail RFC 7807 com 400 Bad Request para IllegalArgumentException")
    void deveRetornarBadRequestParaIllegalArgumentException() throws Exception {
        mockMvc.perform(get("/test/business-error"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Requisicao Invalida"))
                .andExpect(jsonPath("$.detail").value("Parametro invalido informado"));
    }

    @Test
    @DisplayName("Deve retornar ProblemDetail RFC 7807 com 422 Unprocessable Entity para IllegalStateException")
    void deveRetornarUnprocessableEntityParaIllegalStateException() throws Exception {
        mockMvc.perform(get("/test/state-error"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.title").value("Regra de Negocio Violada"))
                .andExpect(jsonPath("$.detail").value("Operacao invalida para o estado atual"));
    }

    @Test
    @DisplayName("Deve retornar ProblemDetail RFC 7807 com 404 Not Found para recurso inexistente")
    void deveRetornarNotFoundParaRotaInexistente() throws Exception {
        mockMvc.perform(get("/test/rota-inexistente"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Recurso Nao Encontrado"));
    }
}
