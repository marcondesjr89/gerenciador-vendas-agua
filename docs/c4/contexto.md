# Diagrama C4 — Nível 1: Contexto de Sistema

**Projeto:** Gerenciador de Vendas de Água Mineral (Marketplace & PDV Modular)  
**Versão:** 2.0.0 (Projeto A — Full Cycle)  
**Rastreabilidade:** RNF-M4.1

---

## 1. Visão Geral de Contexto

O diagrama de contexto descreve as fronteiras do sistema, seus usuários (atores) e os sistemas externos ou clientes com os quais interage.

```mermaid
flowchart TD
    subgraph Users ["Atores e Usuários"]
        OPERATOR["👤 Atendente / Operador\n(Registra pedidos no balcão e gerencia entregas)"]
        ADMIN["👤 Administrador\n(Gerencia catálogo, preços e audita estoque)"]
        CUSTOMER["👤 Cliente Final\n(Consumidor que compra água mineral e troca vasilhames)"]
    end

    subgraph System ["Fronteira do Sistema"]
        APP["📦 Gerenciador de Vendas de Água Mineral\n[Monólito Modular — Spring Boot / Java 21]\nAutomatiza vendas, estoque dual de vasilhames,\npagamentos idempotentes e prévia de cupom fiscal."]
    end

    subgraph External ["Sistemas e Ferramentas Externas"]
        K6["⚙️ Ferramenta de Carga k6\n(Bateria de testes em 3 estágios)"]
        MONITOR["📊 Prometheus / Grafana\n(Coleta de métricas RED via Actuator)"]
        FISCAL_HW["🖨️ Impressora Fiscal / Térmica (Fase 2)\n(Impressão física de cupom fiscal no balcão)"]
    end

    OPERATOR -->|"Abre pedidos, consulta clientes e ajusta estoque [HTTPS/REST]"| APP
    ADMIN -->|"Cadastra produtos e audita estoque dual [HTTPS/REST]"| APP
    CUSTOMER -.->|"Solicita compra via balcão ou telefone"| OPERATOR
    K6 -->|"Executa testes de carga sintéticos (100k pedidos) [REST JSON]"| APP
    APP -->|"Expõe métricas RED via /actuator/prometheus [Scrape]"| MONITOR
    APP -.->|"Emite comandos de impressão via FiscalReceiptPort (Fase 2)"| FISCAL_HW
```

---

## 2. Elementos do Diagrama

| Elemento | Tipo | Descrição |
| :--- | :--- | :--- |
| **Atendente / Operador** | Pessoa | Usuário operacional responsável pelo fluxo ágil de vendas, seleção de itens e baixa de vasilhames. |
| **Administrador** | Pessoa | Usuário gestor responsável pela gestão de catálogo, litragens, auditoria de estoque e preços. |
| **Gerenciador de Vendas de Água** | Sistema de Software | Monólito Modular API-First que encapsula as regras de negócio de catálogo, pedidos e pagamentos. |
| **k6 Load Testing** | Sistema Externo | Suite de testes de carga para avaliação de latência p95 sobre base de 100 mil pedidos sintéticos. |
| **Prometheus** | Sistema Externo | Plataforma de observabilidade que realiza *scraping* periódico das métricas em `/actuator/prometheus`. |
| **Impressora Fiscal (Fase 2)** | Dispositivo Externo | Hardware físico que receberá eventos de impressão através do gancho desacoplado `FiscalReceiptPort`. |
