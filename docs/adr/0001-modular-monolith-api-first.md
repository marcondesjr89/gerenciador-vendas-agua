# ADR 0001: Adoção de Monólito Modular API-First

- **Status:** Aceito
- **Data:** 2026-11-06
- **Decisores:** Marcondes Jr.
- **Rastreabilidade:** RNF-M1.2, RNF-M4.1, Módulo 1 (Projeto A)

---

## Contexto e Declaração do Problema

O sistema automatiza a operação comercial e logística de depósitos de água mineral (catálogo, estoque dual de vasilhames, pedidos balcão/entrega e pagamentos). Para a entrega do **Projeto Final Full Cycle (Nível A)** sob um prazo de 8 dias, é imperativo escolher uma arquitetura que ofereça:
1. Fronteiras de domínio estritas e de alta coesão.
2. Baixo acoplamento entre módulos.
3. Transacionalidade ACID garantida (baixa atômica de estoque ao criar ou cancelar pedidos).
4. Facilidade de execução local via contêineres Docker e testes de carga em 3 estágios com k6.
5. Manutenibilidade e simplicidade operacional.

---

## Opções Consideradas

1. **Opção 1:** Microsserviços Distribuídos (serviços separados para Catálogo, Pedidos e Pagamentos em contêineres e bancos de dados isolados).
2. **Opção 2:** Monólito em Camadas Tradicional (arquitetura em camadas clássica com controllers, services e repositórios globais).
3. **Opção 3 (Escolhida):** **Monólito Modular API-First** (um único artefato executável organizado em módulos explícitos: `catalog`, `orders`, `payment` e `platform`).

---

## Comparativo de Decisão e Alternativas Descartadas

### Por que a Opção 1 (Microsserviços) foi Descartada?
- **Complexidade de Transações Distribuídas:** A criação e o cancelamento de pedidos exigem mutação de estoque dual. Em microsserviços, isso exigiria orquestração SAGA ou Two-Phase Commit (2PC), introduzindo consistência eventual e estados intermediários complexos desnecessários para a escala atual.
- **Sobrecarga de Rede e Latência:** Múltiplos saltos HTTP/gRPC entre serviços internos prejudicariam a meta de latência p95 (`< 200ms`) nos testes de carga k6.
- **Custo Operacional e de Deploy:** Manter múltiplos pipelines de CI, service discovery e containers múltiplos geraria complexidade acidental desproporcional.

### Por que a Opção 2 (Monólito em Camadas Tradicional) foi Descartada?
- **Acoplamento Anárquico:** Sem fronteiras explícitas, serviços de domínios diferentes passam a chamar repositórios uns dos outros sem controle, gerando o clássico *"Big Ball of Mud"*.
- **Dificuldade de Evolução:** Se no futuro um módulo precisar ser escalado independentemente, a refatoração seria dolorosa e de alto risco.

---

## Decisão Aprovada: Monólito Modular API-First

Decidimos estruturar o sistema como um **Monólito Modular API-First**:
- O código reside em um único repositório e executa em uma única JVM (Java 21 LTS com Spring Boot 3.x).
- Cada contexto (`catalog`, `orders`, `payment`) é encapsulado em seu próprio pacote com 4 subcamadas bem definidas: `api`, `application`, `domain` e `infrastructure`.
- A comunicação inter-módulos ocorre estritamente através das portas públicas da camada `application`, nunca acessando diretamente repositórios ou entidades de outro módulo.
- A camada transversal `platform` fornece serviços de infraestrutura compartilhada (exceções RFC 7807, observabilidade RED/Prometheus e Feature Flags).

---

## Consequências

### Consequências Positivas:
- **Transacionalidade Simples e Confiável:** Operações complexas (como baixa de estoque ao fechar pedido ou estorno no cancelamento) utilizam transações locais ACID (`@Transactional`), garantindo consistência imediata sem risco de perda de galões.
- **Performance Elevada (p95):** Sem latência de rede entre chamadas internas, viabilizando responder requisições de consulta e escrita dentro do SLO estrito de 200ms.
- **Deploy Simplificado em 1-Comando:** O arquivo `docker-compose.yml` sobe toda a plataforma (`postgres`, `redis` e `app`) com rapidez e determinismo.
- **Caminho Claro para Futura Extração:** Caso o módulo `payment` ou `catalog` precise ser escalado isoladamente no futuro, os limites já estão demarcados, tornando a extração trivial.

### Consequências Negativas / Mitigações:
- **Risco de Vazamento de Dependências:** Desenvolvedores podem ser tentados a importar classes de infraestrutura de outros módulos.  
  *Mitigação:* Uso de visibilidade de pacote (`package-private`), testes arquiteturais e convenções rígidas de isolamento.
