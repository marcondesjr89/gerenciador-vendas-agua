# Registro de Uso de Inteligência Artificial — Engenharia & Decisões

**Projeto:** Gerenciador de Vendas de Água Mineral (Marketplace & PDV Modular)  
**Versão:** 2.0.0 (Projeto A — Full Cycle)  
**Diretriz Oficial:** Conforme estabelecido no documento `docs/artefato.md`, o uso de IA generativa é incentivado como ferramenta de suporte cognitivo e exploração técnica, desde que todas as decisões sejam justificadas, compreendidas e defendidas pelo desenvolvedor (*"Código que você não sabe explicar não conta como seu"*).

---

## 🧭 Princípios de Utilização da IA no Projeto

1. **A IA como Assistente de Exploração:** A IA foi utilizada para sugerir alternativas de design patterns, comparar trade-offs arquiteturais e acelerar a criação de testes de borda repetitivos.
2. **Engenheiro como Tomador de Decisão:** Nenhuma sugestão foi adotada sem análise de conformidade com os princípios SOLID, limites da rubrica do Projeto A e Clean Code.
3. **Auditoria Rigorosa de Segurança (AppSec):** Toda geração de código foi submetida a verificações mecânicas de transacionalidade (`@Transactional`), concorrência (`@Version`), tratamento RFC 7807 e ausência de injeção SQL.

---

## 📝 Prompts Decisivos e Impactos Arquiteturais

### Prompt 01: Escolha Arquitetural — Monólito Modular API-First vs. Microsserviços
- **Contexto:** Definição da arquitetura base para o Projeto A considerando o prazo de entrega (8 dias) e os requisitos de alta coesão e baixo acoplamento.
- **Prompt:**
  > *"Analise o trade-off entre construir este sistema como microsserviços distribuídos versus um Monólito Modular API-First em Java 21 / Spring Boot 3.4. Considere transacionalidade entre estoque e pedidos, complexidade de deploy no Docker, latência de rede e facilidade de teste com k6."*
- **Alternativas Sugeridas:**
  1. *Microsserviços:* Catálogo, Pedidos e Pagamento em repositórios e contêineres separados via HTTP/gRPC.
  2. *Monólito Modular:* Um único artefato compilado com pacotes isolados (`catalog`, `orders`, `payment`, `platform`) com contratos internos explícitos.
- **Decisão e Justificativa do Engenheiro:** Adotamos o **Monólito Modular API-First** (registrado na ADR 0001). Microsserviços trariam complexidade acidental desproporcional (two-phase commit, eventual consistency, falhas parciais de rede) sem ganho de escala real no escopo do projeto. O monólito modular garante limites limpos e permite refatoração futura para microsserviços sem atrito, caso necessário.

---

### Prompt 02: Modelagem de Estoque Dual de Vasilhames Retornáveis (Strategy Pattern)
- **Contexto:** A gestão de depósitos de água exige regras distintas de movimentação para galões completos, refis (onde o cliente devolve o vasilhame vazio), venda de vasilhames vazios avulsos e produtos descartáveis.
- **Prompt:**
  > *"Como modelar no domínio a movimentação de estoque de galões cheios vs vasilhames vazios para evitar múltiplos if/else espalhados pelo serviço quando novos tipos de embalagens ou produtos forem criados?"*
- **Alternativas Sugeridas:**
  1. *Condicionais if/switch* no `StockMovementService`.
  2. *State Pattern* na entidade de produto.
  3. *Strategy Pattern* desacoplado via interface `StockMovementStrategy` injetada dinamicamente pelo Spring.
- **Decisão e Justificativa do Engenheiro:** Adotamos o **Strategy Pattern** (registrado na ADR 0002). Cada estratégia (`FullWaterStockStrategy`, `RefillStockStrategy`, `EmptyBottleStockStrategy`, `DisposableStockStrategy`) isola 100% da sua regra matemática de cheios e vazios. Cumpre rigorosamente o Princípio Aberto/Fechado (OCP) e o Princípio da Responsabilidade Única (SRP).

---

### Prompt 03: Padronização Universal para 100% Inglês Técnico
- **Contexto:** O projeto apresentava termos mistos (código bilíngue) entre classes e banco de dados.
- **Prompt:**
  > *"Como padronizar 100% dos nomes de classes, atributos, métodos, enums, migrations Flyway e entidades para o inglês técnico sem perder a clareza do modelo de negócio brasileiro de distribuidora de água mineral?"*
- **Decisão e Justificativa do Engenheiro:** Todo o código, schemas Flyway (`products`, `orders`, `order_items`, `payments`) e APIs foram migrados para o inglês técnico universal (`FULL_WATER`, `REFILL`, `EMPTY_BOTTLE`, `DISPOSABLE`). A regra foi formalizada na convenção de engenharia do projeto e no template global de scaffolds (`engineering-standards.md`).

---

### Prompt 04: Shift-Left de CI e Governança no Primeiro Dia
- **Contexto:** Questionamento do desenvolvedor sobre a pertinência de manter o GitHub Actions tardiamente no backlog (ID-013) em vez de validar a cada push.
- **Prompt:**
  > *"O GitHub Actions serve para validar o código a cada push ou PR. Deveríamos adiar para o ID 13 ou adiantar para ter feedback contínuo desde o início?"*
- **Decisão e Justificativa do Engenheiro:** Adiantamos o workflow do CI para execução imediata em todo push e pull request. Isso garante que todo commit seja atestado contra o ambiente limpo do Ubuntu no GitHub Actions, garantindo *CI Verde desde o primeiro dia*, em conformidade com o critério M2 da avaliação.
