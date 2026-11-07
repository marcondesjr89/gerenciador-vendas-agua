package com.distribuidora.agua.platform.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * SpringDoc OpenAPI 3.1 configuration and API metadata.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Gerenciador de Vendas de Água Mineral - API")
                        .version("2.0.0")
                        .description("API RESTful do Monólito Modular de Gerenciamento de Vendas, Estoque Dual, Pedidos e Pagamentos Idempotentes.")
                        .contact(new Contact()
                                .name("Equipe de Engenharia Distribuidora de Água")
                                .email("engenharia@distribuidora-agua.com.br")
                                .url("https://github.com/marcondesjr89/gerenciador-vendas-agua"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Ambiente Local de Desenvolvimento"),
                        new Server().url("https://api.distribuidora-agua.com.br").description("Ambiente de Produção (PaaS)")
                ))
                .tags(List.of(
                        new Tag().name("Catalog").description("Catálogo de Produtos, Estoque Dual (Cheio/Vazio) e Ajustes"),
                        new Tag().name("Orders").description("Ciclo de Vida de Pedidos, Máquina de Estados, Frete Dinâmico e Estorno"),
                        new Tag().name("Payments").description("Processamento de Pagamentos Idempotente com Redis Lock e PostgreSQL"),
                        new Tag().name("Platform").description("Governança em Tempo de Execução, Feature Flags e Ganchos Fiscais"),
                        new Tag().name("Observability").description("Observabilidade, Métricas RED e Healthchecks")
                ));
    }
}
