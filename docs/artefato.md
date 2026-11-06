# Diretrizes e Especificações Oficiais — Projeto Final Full Cycle

> **Módulo 4 • Encontro 24: Níveis, Entrega e Avaliação**  
> **Fonte Oficial:** `ProjetoFInal.pdf` (Slides de aula — Prof. Henrique Lopes Lima)  
> **Contato / Convite de repositório privado:** `henriquelopeslima.hll@gmail.com`

---

## 1. Visão Geral e Filosofia do Projeto

- **Objetivo:** Projeto prático abrangente que compõe a nota de conclusão do curso.
- **Cobertura total do curso:** Do contêiner ao teste de carga.
- **Aproveitamento:** Não é um trabalho novo iniciado do zero; é o seu projeto integrando e consolidando as camadas vistas ao longo dos módulos (as parcelas do Módulo 4 já são entregáveis daqui: ADR, contextos, C4, OpenAPI, plano de p95, k6). O projeto final consolida, não recomeça.
- **Lema central:** *"Mostrar engenharia, não tamanho."*

---

## 2. Cronograma e Prazos Oficiais

| Data | Evento / Marco | Observações |
| :--- | :--- | :--- |
| **05/11** | **Diretrizes, Níveis e Escolha** | Alinhamento de regras, escopos e tomada de decisão |
| **Até 13/11** | **Entrega Oficial** | Envio direto pelo **Moodle**. Commits após 13/11 **não** serão avaliados |
| **16/11** | **Apresentações ao Vivo** | Aula síncrona para apresentação dos projetos |

> [!WARNING]
> **Atenção ao Prazo:** São **8 dias** de execução. Se o escopo planejado não cabe nesse intervalo, **corte o escopo imediatamente**.

---

## 3. Formato de Trabalho e Regras de Avaliação

- **Composição da equipe:** Individual ou em grupo de até **3 pessoas**.
- **Equiparação de escopo:** O escopo técnico exigido é exatamente o **mesmo** nos dois casos (individual ou trio).
- **Divisão do trabalho em grupo:** Divida por **módulo funcional do sistema**, e **não** por tarefa de nota (ex: um cuida de pedidos, outro de catálogo, etc.).
- **A nota é estritamente INDIVIDUAL:**
  - Commits e PRs devem estar no nome de cada integrante (o histórico do Git é a evidência real de contribuição).
  - É obrigatório incluir um arquivo `CONTRIBUTORS.md` detalhando com precisão o que cada integrante desenvolveu.
  - Na apresentação, cada aluno defende a sua própria parte.
  - *"Quem não aparece no histórico não tem o que ser avaliado."*

---

## 4. Comparativo de Níveis: Projeto A × Projeto B

| Característica | Projeto A (Avançado) | Projeto B (Simples) |
| :--- | :--- | :--- |
| **Escopo do Sistema** | Marketplace modular (monólito modular): catálogo, pedidos e pagamento com fronteiras explícitas (não apenas pastas soltas) | Uma API de domínio único (pedidos, biblioteca, agenda ou o tema de sua preferência) com fluxo principal funcionando de ponta a ponta |
| **Massa de Dados** | Banco com volume: pelo menos **100 mil pedidos sintéticos** | Sem exigência de volume sintético de banco |
| **Decomposição** | Módulos bem demarcados com fronteiras claras | Sem necessidade de decomposição em módulos complexos |
| **Arquitetura & Docs** | ADR com alternativa descartada, C4 (contexto e contêiner), OpenAPI 3.1 com lint no CI | README completo do primeiro dia, 1 ADR no formato MADR, OpenAPI do endpoint principal |
| **Performance (k6)** | Teste de carga k6 em **3 estágios**, plano de p95 (cache + fila/idempotência) e relatório antes/depois | 1 smoke test com k6 com percentil p95 medido |
| **Princípio Norteador** | *"Mesma espinha dorsal, mais profundidade e artefatos"* | *"Simples não é incompleto: o fluxo tem que funcionar de verdade"* |

### Critérios para Escolha:
1. **Projeto B não é desistência:** Um Projeto B impecável vale mais que um Projeto A entregue pela metade.
2. **Momento do código:**
   - Escolha **A** se já possui CI e testes rodando previamente.
   - Escolha **B** se está iniciando o desenvolvimento nesta semana.
3. **Declaração:** Declare o nível escolhido logo na **primeira linha do README.md**.

---

## 5. Especificações Detalhadas — Projeto A (Nível Avançado)

### 5.1 O Sistema
- Marketplace arquitetado como monólito modular abrangendo: **Catálogo**, **Pedidos** e **Pagamento**.
- Módulos com fronteira explícita (contratos claros de comunicação, sem acoplamento direto ou pastas soltas).
- Banco de dados populado com volume real: mínimo de **100.000 pedidos sintéticos**.

### 5.2 Módulos e Entregáveis

| Módulo de Origem | Entregável Obrigatório | Ferramentas Sugeridas / Exemplos |
| :--- | :--- | :--- |
| **M1: Base e Qualidade** | • Docker Compose que inicializa o ambiente com 1 único comando<br>• Código aplicando princípios SOLID<br>• Pelo menos 1 Design Pattern justificado | Docker Compose, ESLint |
| **M1: Persistência** | • 1 Database Pattern aplicado e formalmente registrado | Prisma, TypeORM ou Knex |
| **M2: Testes** | • Testes unitários<br>• Testes de integração<br>• Testes de contrato de API | Vitest ou Jest, Testcontainers, Pact |
| **M2: Cobertura** | • Cobertura de testes (*coverage*) acima do mínimo declarado no próprio pipeline | Vitest coverage (v8), Codecov |
| **M3: Integração Contínua** | • Pipeline de CI com lint, testes automatizados, verificação de coverage e análise de SCA (dependências) | GitHub Actions, npm audit, Trivy |
| **M3: Configuração & Segurança** | • Dois ambientes configurados<br>• Configurações injetadas via ambiente<br>• Segredos estritamente fora do código-fonte | dotenv, GitHub Environments e Secrets, gitleaks |
| **M3: Observabilidade** | • Logs estruturados<br>• Métricas padrão RED (Rate, Errors, Duration)<br>• Pelo menos 1 distributed trace<br>• Pelo menos 1 SLO formalmente declarado | pino, Prometheus + Grafana, OpenTelemetry + Jaeger |
| **M3: Deploy & Releases** | • 1 Feature Flag implementada<br>• Estratégia de deploy escolhida e documentada | Unleash ou GrowthBook, Render ou Railway |
| **M4: Arquitetura** | • ADR (Architecture Decision Record) incluindo a alternativa descartada<br>• Modelo C4: Diagramas de Contexto e de Contêiner | Template MADR, Mermaid C4, Structurizr |
| **M4: Contratos de API** | • Especificação OpenAPI 3.1 para catálogo e pedido<br>• Validação via lint de especificação no pipeline de CI | Spectral, Redocly CLI, Swagger UI |
| **M4: Desempenho & Resiliência** | • Plano de p95: estratégia de cache com tolerância e garantia de idempotência no processamento de filas | Redis, BullMQ ou RabbitMQ |
| **M4: Testes de Carga** | • Teste de carga k6 executado em **3 estágios**<br>• Relatório técnico contendo identificação de gargalos e medições de antes e depois | k6, clinic.js, `EXPLAIN ANALYZE` |

---

## 6. Especificações Detalhadas — Projeto B (Nível Simples)

### 6.1 O Sistema
- Uma API REST com **domínio único** (ex.: sistema de pedidos, controle de biblioteca, agenda de serviços ou outro domínio à escolha).
- **Um fluxo principal funcionando de ponta a ponta** com persistência real.
- Não exige decomposição em monólito modular nem geração massiva de 100 mil registros sintéticos.
- Regra de ouro: *Simples não significa incompleto; o fluxo básico precisa funcionar perfeitamente.*

### 6.2 Módulos e Entregáveis

| Módulo de Origem | Entregável Obrigatório | Ferramentas Sugeridas / Exemplos |
| :--- | :--- | :--- |
| **M1: Base e Qualidade** | • Docker Compose que sobe com 1 único comando<br>• Boas práticas e SOLID<br>• 1 Design Pattern justificado no código | Docker Compose, ESLint |
| **M2: Testes** | • Testes unitários do domínio<br>• Pelo menos 1 teste de integração ponta a ponta | Vitest ou Jest, Supertest |
| **M2: Cobertura** | • Cobertura mínima de testes formalmente declarada no projeto e respeitada | Vitest coverage (v8) |
| **M3: CI** | • Pipeline de CI com status verde garantido: instalação de dependências, lint e execução de testes | GitHub Actions, ESLint |
| **M3: Configuração & Segurança** | • Arquivo `.env.example` versionado no repositório<br>• Nenhum segredo commitado no repositório | dotenv, gitleaks |
| **M3: Observabilidade** | • Logs estruturados<br>• 1 métrica de aplicação coletada<br>• 1 SLO simples declarado | pino, prom-client |
| **M4: Documentação & Decisão** | • `README.md` completo desde o primeiro dia de submissão<br>• 1 ADR formalmente documentada | Markdown, template MADR |
| **M4: API & Carga** | • Especificação OpenAPI documentando o endpoint principal<br>• 1 smoke test executado via k6 com medição do percentil p95 | Swagger Editor, Spectral, k6 |

---

## 7. Estrutura de Diretórios Esperada no Repositório

```text
.
├── docker-compose.yml
├── README.md
├── docs/
│   ├── adr/
│   │   └── 0001-decisao.md
│   ├── c4/                 # Obrigatório no Projeto A (contexto e contêiner); omitido no B
│   │   └── contexto.md
│   ├── openapi.yaml
│   ├── desempenho.md       # Relatório de medições de carga / gargalos
│   └── ia.md               # Registro obrigatório dos prompts de IA utilizados
├── src/
├── tests/
├── CONTRIBUTORS.md         # Detalhamento de atribuições por integrante
└── .github/
    └── workflows/
        └── ci.yml
```

> [!NOTE]
> **Diferença de estrutura entre níveis:**
> - No **Projeto A**: a pasta `docs/` possui a árvore completa (`adr/`, `c4/` com contexto e contêiner, `openapi.yaml`, `desempenho.md`, `ia.md`).
> - No **Projeto B**: a pasta `docs/` inclui `README`, 1 ADR, `openapi.yaml`, `desempenho.md` e `ia.md`, **sem** a necessidade da pasta `c4/`.

---

## 8. Protocolo de Entrega (Até 13/11 via Moodle)

Para a submissão no Moodle, os seguintes itens devem ser entregues:

1. **[01] Link do repositório:** URL pública no GitHub (ou convite adicionando o professor se privado: `henriquelopeslima.hll@gmail.com`).
2. **[02] README.md:** Declarando logo no início o nível do projeto (A ou B), instruções claras de como subir o ambiente (`docker compose up`) e como rodar a suíte de testes.
3. **[03] Link da execução verde do CI:** URL direta comprovando o pipeline passando com sucesso no GitHub Actions.
4. **[04] Documentação chave:**
   - `docs/desempenho.md` (com os números de carga, gargalos identificados e análises).
   - `CONTRIBUTORS.md` (com a lista de membros e suas responsabilidades).
   - `docs/ia.md` (com os prompts de IA relevantes).

> [!CAUTION]
> **Prazo Impreterível:** Qualquer commit realizado após as 23h59 do dia **13/11 NÃO será avaliado**.

---

## 9. Apresentação (Opcional, com Bônus)

- **Caráter:** Apresentar é **opcional**, mas bonifica: concede **até 10 pontos extras**.
- **Formatos aceitos:**
  - **Ao vivo:** Na aula síncrona do dia **16/11**.
  - **Vídeo gravado:** Enviado diretamente pelo Moodle.
- **Prazo para envio do vídeo:** Segue rigorosamente o prazo geral de entrega — **13/11**.
- **Roteiro estrito de 10 minutos (divisão de tempo recomendada):**
  - **1 min:** Apresentação do problema de negócio e do nível escolhido (A ou B).
  - **3 min:** Demonstração prática ao vivo: aplicação sobe, roda comandos e responde às requisições.
  - **3 min:** Explicação de uma decisão técnica relevante e a alternativa técnica que foi descartada.
  - **3 min:** Apresentação dos números obtidos (testes de carga/p95) e reflexão sobre o que faria diferente.

---

## 10. Rubrica de Avaliação e Composição da Nota

A régua de avaliação segue critérios técnicos com pesos bem definidos. A mesma rubrica aplica-se tanto ao Projeto A quanto ao Projeto B — o que muda é o tamanho e a profundidade da evidência, não o nível de rigor.

| Critério | Descrição Avaliada | Peso |
| :--- | :--- | :---: |
| **M1: Funciona, Código e Design** | Aplicação operacional, código limpo, aplicação de princípios SOLID, patterns adotados e orquestração via contêineres | **35 pontos** |
| **M2: Testes e CI Verde** | Cobertura de testes efetiva (unitários/integração/contrato), ausência de testes sem asserção e esteira de CI funcional com build verde | **20 pontos** |
| **M3: Ambientes, Segredos e Observabilidade** | Isolamento de configurações, ausência de credenciais/segredos expostos, estruturação de logs, métricas e SLOs | **15 pontos** |
| **M4: Arquitetura, Documentação e Desempenho** | Clareza dos diagramas arquiteturais, especificação OpenAPI com lint, ADR fundamentada, testes com k6 e análise de p95 | **30 pontos** |
| **Total** | **Nota máxima padrão do projeto** | **100 pontos** |
| *Bônus* | *Apresentação síncrona em 16/11 ou vídeo enviado até 13/11* | *+ até 10 pontos* |

---

## 11. Diretrizes para o Uso de Inteligência Artificial

- **Permitido e esperado:** O uso de assistentes de IA generativa fez parte de metade do conteúdo do curso.
- **Registro obrigatório:** Documente no arquivo `docs/ia.md` os principais prompts que geraram impacto nas decisões e na arquitetura.
- **Papel da IA:** A IA sugere opções e alternativas; a responsabilidade da decisão técnica e do design continua sendo do engenheiro.
- **Princípio avaliativo fundamental:** *"Código que você não sabe explicar não conta como seu."*

---

## 12. O que Mais Derruba Nota (Erros Fatais a Evitar)

1. **[01] Escopo inflado e nada funcionando:** Querer abraçar arquiteturas gigantes e não ter o fluxo básico operando de ponta a ponta.
2. **[02] CI vermelha ou inexistente:** Repositório sem pipeline ou com pipeline quebrando na branch principal.
3. **[03] README que não deixa a aplicação subir:** Instruções incompletas, dependências faltando ou comandos que falham ao rodar localmente.
4. **[04] Segredos commitados no repositório:** Chaves de API, senhas de banco ou tokens trafegando no controle de versão.
5. **[05] Testes sem asserção e números sem medição:** Testes "falsos" que apenas executam linhas para inflar cobertura sem validar resultados (`expect(...)`), ou dados de desempenho inventados sem relatório de teste com k6.

---

## 13. Check-list Imediato ("Decida Antes de Sair")

Dedique 15 minutos iniciais para definir:
- [ ] **[01] Nível definido:** Projeto A (Avançado) ou Projeto B (Simples);
- [ ] **[02] Formação da equipe:** Individual ou grupo (com até 3 nomes formalizados);
- [ ] **[03] Repositório oficial:** Repositório criado e configurado para entrega;
- [ ] **Passo extra imediato:** Abrir o primeiro Pull Request com o `README.md` inicial já declarando o nível do projeto na primeira linha.
