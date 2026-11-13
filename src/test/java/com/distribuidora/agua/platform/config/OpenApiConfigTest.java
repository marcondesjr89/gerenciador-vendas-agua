package com.distribuidora.agua.platform.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OpenApiConfig Unit Tests")
class OpenApiConfigTest {

    @Test
    @DisplayName("Should configure custom OpenAPI metadata matching Level A specification")
    void shouldConfigureCustomOpenApiMetadata() {
        OpenApiConfig config = new OpenApiConfig();
        OpenAPI openAPI = config.customOpenAPI();

        assertThat(openAPI).isNotNull();
        assertThat(openAPI.getInfo()).isNotNull();
        assertThat(openAPI.getInfo().getTitle()).isEqualTo("Gerenciador de Vendas de Água Mineral - API");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("2.0.0");
        assertThat(openAPI.getInfo().getDescription()).isNotBlank();
        assertThat(openAPI.getInfo().getContact()).isNotNull();
        assertThat(openAPI.getInfo().getContact().getEmail()).isEqualTo("engenharia@distribuidora-agua.com.br");
        assertThat(openAPI.getServers()).hasSize(3);
        assertThat(openAPI.getServers().getFirst().getUrl()).isEqualTo("/");
        assertThat(openAPI.getTags()).hasSize(5);
        assertThat(openAPI.getTags())
                .extracting("name")
                .containsExactlyInAnyOrder("Catalog", "Orders", "Payments", "Platform", "Observability");
    }
}
