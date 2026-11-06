# ADR 0002: Strategy Pattern para Estoque Dual e Data Mapper JPA

- **Status:** Aceito
- **Data:** 2026-11-06
- **Decisores:** Marcondes Jr.
- **Rastreabilidade:** RF-002, RNF-M1.2, RNF-M1.3, Módulo 1 (Projeto A)

---

## Contexto e Declaração do Problema

Depósitos de água mineral operam com produtos de naturezas físicas distintas que afetam o estoque de formas assimétricas:
1. **Água Completa (`FULL_WATER`):** Venda do líquido acompanhado do vasilhame (decrementa galões cheios).
2. **Refil com Troca (`REFILL`):** O cliente traz o vasilhame vazio e leva o cheio (decrementa galões cheios e incrementa vasilhames vazios no depósito).
3. **Vasilhame Vazio Avulso (`EMPTY_BOTTLE`):** Venda apenas do casco plástico (decrementa vasilhames vazios).
4. **Descartável (`DISPOSABLE`):** Garrafas não retornáveis (decrementa cheios sem impacto em vazios).

Além disso, em operações concorrentes de venda balcão e pedidos simultâneos, dois operadores podem tentar vender o último galão ao mesmo tempo, exigindo controle de concorrência e integridade relacional.

---

## Opções Consideradas

### Para as Regras de Negócio de Estoque:
1. **Opção A:** Condicionais `if/switch` centralizadas dentro de um método gigante no serviço.
2. **Opção B (Escolhida):** **Strategy Pattern** desacoplado via interface `StockMovementStrategy` com implementações polimórficas por `ProductType`.

### Para a Persistência de Dados:
1. **Opção C:** Active Record ou JDBC puro.
2. **Opção D (Escolhida):** **Data Mapper via Spring Data JPA + Hibernate** com controle de concorrência otimista (`@Version`) e migrações versionadas com **Flyway**.

---

## Comparativo de Decisão e Alternativas Descartadas

### Por que a Opção A (if/switch) foi Descartada?
- **Violação do Princípio Aberto/Fechado (OCP):** Toda vez que a distribuidora criar uma nova modalidade de produto (ex: galão de 10L retornável especial ou consignação), o método central teria que ser reaberto e modificado, gerando risco de regressão nos produtos existentes.
- **Alta Complexidade Ciclomática:** O método do serviço ficaria repleto de branches aninhadas e difícil de testar de forma isolada.

### Por que a Opção C (Active Record / JDBC puro) foi Descartada?
- **Falta de Abstração e Concorrência Otimista:** Implementar controle de versão concorrente (`SELECT ... FOR UPDATE` manual ou checagem manual de versão) no JDBC puro exigiria muito código boilerplate (*plumbing code*), desviando o foco da regra de negócio.
- O padrão **Data Mapper** separa a representação em memória do domínio das tabelas relacionais do banco.

---

## Decisão Aprovada: Strategy Pattern + Data Mapper JPA

Decidimos adotar:
1. **Strategy Pattern (`StockMovementStrategy`):**
   - Interface declarando: `boolean supports(ProductType)`, `void debitStock(Product, int)` e `void creditStock(Product, int)`.
   - 4 implementações isoladas registradas como Spring Beans:
     - `FullWaterStockStrategy`
     - `RefillStockStrategy`
     - `EmptyBottleStockStrategy`
     - `DisposableStockStrategy`
   - O `StockMovementService` apenas resolve e despacha para a estratégia correta via injeção de dependência (`List<StockMovementStrategy>`), cumprindo rigorosamente SRP e OCP.
2. **Data Mapper (Spring Data JPA) com Concorrência Otimista:**
   - Entidade `Product` anotada com `@Version private Long version`.
   - Caso duas requisições tentem debitar o mesmo produto simultaneamente com versões desatualizadas, o JPA lança automaticamente `ObjectOptimisticLockingFailureException`, prevenindo perdas de atualização (*Lost Updates*).
   - DDL versionado estritamente por migrações Flyway (`V1__init_schema.sql`).

---

## Consequências

### Consequências Positivas:
- **Testabilidade Perfeita:** Cada strategy possui sua própria suíte de testes unitários isolada, cobrindo cenários de sucesso e lançamento de `InsufficientStockException` em poucos milissegundos.
- **Extensibilidade Sem Atrito:** Novos tipos de produtos podem ser adicionados simplesmente criando uma nova classe que implementa `StockMovementStrategy`, sem alterar nenhuma linha do serviço existente.
- **Segurança Transacional:** O controle otimista de concorrência impede vendas em falso mesmo sob picos de requisições concorrentes.

### Consequências Negativas / Mitigações:
- **Maior Número de Classes:** Criação de mais arquivos de classes no pacote de domínio.  
  *Mitigação:* A organização coesa e o ganho de legibilidade superam amplamente o pequeno aumento de arquivos.
