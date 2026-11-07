# ADR 0003: Feature Flags em Tempo de Execução e Estratégia de Deploy

- **Status:** Aceito
- **Data:** 2026-11-07
- **Decisores:** Marcondes Jr.
- **Rastreabilidade:** RF-006, RNF-M3.4, Módulo 3 (Projeto A)

---

## Contexto e Declaração do Problema

O sistema Gerenciador de Vendas de Água Mineral possui funcionalidades comerciais e fiscais cujo comportamento deve ser flexível em produção:
1. **Gancho de Cupom Fiscal (`feature.fiscal-receipt-preview`):** Durante a Fase 1 e em ambientes de homologação sem impressora física conectada, o sistema deve ser capaz de emitir o payload estruturado de cupom fiscal para visualização e conferência. Em contingência ou necessidade de bypass, a funcionalidade deve poder ser desativada sem parada de serviço.
2. **Promoção de Frete Grátis (`feature.free-delivery-promo`):** A regra comercial de isenção de taxa de entrega para pedidos acima de determinado valor deve poder ser ativada ou desativada instantaneamente pelo gestor do depósito (ex: em dias de chuva ou promoções relâmpago).
3. **Estratégia de Deploy em Produção (PaaS: Render / Railway):** É mandatório garantir zero downtime e reversibilidade rápida de versões sem corromper transações de pedidos em andamento.

---

## Opções Consideradas para Feature Flags

1. **Opção 1:** Serviço Externo Dedicado (Unleash / GrowthBook / LaunchDarkly).
2. **Opção 2:** Variáveis Estáticas em `application.properties` (exigem restart completo do container para alteração).
3. **Opção 3 (Escolhida):** **Feature Flags Nativas com API de Gerenciamento em Runtime (`FeatureFlagService`)**:
   - Inicializadas com valores padrão das variáveis de ambiente (`FEATURE_FISCAL_RECEIPT_PREVIEW`, `FEATURE_FREE_DELIVERY_PROMO`).
   - Armazenadas em memória concorrente thread-safe (`ConcurrentHashMap`) no monólito.
   - Gerenciáveis em tempo de execução via endpoints REST autenticados (`GET /api/v1/platform/features` e `PATCH /api/v1/platform/features/{key}`).

---

## Opções Consideradas para Estratégia de Deploy (PaaS)

1. **Opção 1:** *Recreate Deployment* (derruba o container anterior e sobe o novo). Gera downtime perceptível e falhas de requisição.
2. **Opção 2 (Escolhida):** **Rolling / Blue-Green Deployment com Healthcheck Ativo (Render / Railway)**:
   - Ambas as plataformas (Render e Railway) suportam deployments baseados em contêiner Docker com checagem de saúde (`/actuator/health`).
   - O novo contêiner (versão N+1) é iniciado em paralelo enquanto a versão antiga (versão N) continua atendendo o tráfego.
   - O roteador de tráfego só redireciona as requisições quando a rota `/actuator/health` retornar status `UP` (PostgreSQL conectado e migrações Flyway aplicadas).
   - Caso o healthcheck falhe, o container defeituoso é descartado e o tráfego permanece intacto na versão anterior (Rollback instantâneo automático).

---

## Decisão Aprovada

### 1. Feature Flags em Runtime
- Implementação de `FeatureFlagService` expondo métodos fortemente tipados (`isEnabled(FeatureFlagKey)`), desacoplando a lógica de negócio das regras de ativação.
- Endpoints REST `/api/v1/platform/features` para auditoria e controle dinâmico.
- Arquitetura de portas e adaptadores (`FiscalReceiptPort` e `FiscalReceiptPreviewAdapter`) que consulta a feature flag antes de processar o cupom.

### 2. Estratégia de Deploy em Produção (Render / Railway)
- **Estratégia:** *Zero-Downtime Rolling Update*.
- **Configuração de Healthcheck:**
  - Endpoint: `GET /actuator/health`
  - Timeout: 5 segundos
  - Threshold: 2 verificações consecutivas com status 200 OK
- **Compatibilidade com Banco de Dados (Flyway):**
  - Todas as migrações SQL são estritamente aditivas e compatíveis com a versão anterior (evitando remoção destrutiva de colunas em produção), permitindo a convivência transitória entre versões durante o rolling update.

---

## Consequências

### Consequências Positivas:
- **Agilidade Operacional:** Habilitação ou desabilitação de promoções e emissão fiscal em milissegundos sem reiniciar o monólito.
- **Resiliência e Zero Downtime:** Deploy seguro com healthcheck integrado que previne publicações com falhas de inicialização ou regressões críticas.
- **Autonomia e Baixa Complexidade:** Não adiciona custos ou dependências de rede externas a servidores de feature flags terceiros na Fase 1.

### Consequências Negativas / Mitigações:
- *Estado em Memória da Instância:* Caso a aplicação seja escalada horizontalmente para múltiplas réplicas, cada réplica precisaria receber a atualização.
  - *Mitigação:* Para monólito com escala vertical (adequada para até dezenas de milhares de requisições diárias), o estado em memória local é determinístico e instantâneo. Para clusters maiores no futuro (Fase 2), o `FeatureFlagService` poderá usar pub/sub no Redis já configurado no projeto.
