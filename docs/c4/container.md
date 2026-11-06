# Diagrama C4 — Nível 2: Contêineres de Sistema

**Projeto:** Gerenciador de Vendas de Água Mineral (Marketplace & PDV Modular)  
**Versão:** 2.0.0 (Projeto A — Full Cycle)  
**Rastreabilidade:** RNF-M4.1

---

## 1. Visão Geral de Contêineres

O diagrama de contêineres descreve a arquitetura de implantação de alto nível e as responsabilidades dos componentes de software em execução dentro da infraestrutura orquestrada por Docker Compose.

```mermaid
flowchart TD
    subgraph Clients ["Clientes"]
        WEB["🌐 Swagger UI / PDV Web\n[Single Page App / Browser]\nConsome a API REST JSON"]
        LOAD["⚡ k6 Load Generator\n[Script de Carga em 3 Estágios]\nDispara requisições simultâneas"]
    end

    subgraph Compose ["Infraestrutura Docker Compose (1-Comando)"]
        subgraph AppContainer ["Contêiner: agua_api"]
            subgraph Monolith ["Monólito Modular — Spring Boot 3.3.5 / Java 21"]
                CATALOG["📦 Módulo catalog\n(Produtos, litragens, estoque dual cheios/vazios,\nStrategy Pattern e Cache Resiliente)"]
                ORDERS["📦 Módulo orders\n(Abertura de pedidos balcão/entrega, frete,\nmáquina de estados e gancho fiscal)"]
                PAYMENT["📦 Módulo payment\n(Processamento assíncrono com idempotência estrita)"]
                PLATFORM["📦 Módulo platform\n(Erros RFC 7807, RED Metrics, Feature Flags)"]
            end
        end

        subgraph DbContainer ["Contêiner: agua_postgres"]
            DB[("🗄️ PostgreSQL 17\n[Database Relacional]\nPersistência ACID, Flyway migrations e\n100.000 pedidos sintéticos com índices")]
        end

        subgraph CacheContainer ["Contêiner: agua_redis"]
            REDIS[("⚡ Redis 7.4\n[In-Memory Cache & Key-Value]\nCache de Catálogo (com fallback) e\nLock distribuído de Idempotency-Key")]
        end
    end

    WEB -->|"Chamadas REST/JSON [HTTP:8080]"| AppContainer
    LOAD -->|"Requisições sob stress [HTTP:8080]"| AppContainer

    CATALOG -->|"Lê e Grava dados de produtos [JDBC / TCP:5432]"| DB
    CATALOG -->|"Cache de catálogo com fallback [Lettuce / TCP:6379]"| REDIS

    ORDERS -->|"Lê e Grava pedidos e itens [JDBC / TCP:5432]"| DB
    ORDERS -->|"Contrato interno síncrono"| CATALOG

    PAYMENT -->|"Grava transações e status [JDBC / TCP:5432]"| DB
    PAYMENT -->|"Lock de Idempotência [Lettuce / TCP:6379]"| REDIS
    PAYMENT -->|"Notifica liquidação de pagamento"| ORDERS
```

---

## 2. Elementos e Responsabilidades de Implantação

| Contêiner | Tecnologia | Porta | Responsabilidade |
| :--- | :--- | :---: | :--- |
| **`agua_api`** | Eclipse Temurin 21 JRE, Spring Boot 3.3.5 | `8080` | Executa o monólito modular, expõe endpoints REST, processa transações e métricas RED. |
| **`agua_postgres`** | PostgreSQL 17 Alpine | `5432` | Armazena o schema relacional versionado por Flyway (`products`, `orders`, `order_items`, `payments`) e a base volumosa de 100k pedidos. |
| **`agua_redis`** | Redis 7.4 Alpine | `6379` | Camada de cache volátil para listagem de catálogo com fallback gracioso para o banco e locks de idempotência. |
