# Autoria e Contribuições — Projeto Final Full Cycle

**Projeto:** Gerenciador de Vendas de Água Mineral (Marketplace & PDV Modular)  
**Nível:** Projeto A (Avançado)  
**Professor / Avaliador:** Prof. Henrique Lopes Lima (`henriquelopeslima.hll@gmail.com`)  
**Data de Submissão:** Novembro de 2026  

---

## 👨‍💻 Integrante e Responsabilidades

| Nome | GitHub | E-mail de Contato | Módulos e Responsabilidades Desenvolvidas |
| :--- | :--- | :--- | :--- |
| **Marcondes Jr.** | [@marcondesjr89](https://github.com/marcondesjr89) | *(Registrado no Moodle)* | • Arquitetura de Monólito Modular API-First e especificações formais (SRS v2.0.0).<br>• Infraestrutura Docker Compose (PostgreSQL 17, Redis 7.4, multi-stage Dockerfile).<br>• Módulo `platform` (tratamento global de exceções RFC 7807, observabilidade RED/Prometheus e Feature Flags em tempo de execução).<br>• Módulo `catalog` (entidade `Product`, domínio 100% em inglês, Strategy Pattern para controle de estoque dual de vasilhames cheios/vazios e cache Redis com fallback resiliente).<br>• Módulo `orders` (ciclo de vida de pedidos, frete dinâmico, cancelamento transacional com estorno e gancho para impressora fiscal).<br>• Módulo `payment` (processamento assíncrono idempotente com chave `Idempotency-Key`).<br>• Suíte completa de testes automatizados unitários e de integração com cobertura JaCoCo e esteira de CI no GitHub Actions. |

---

## 📜 Declaração de Autoria e Integridade Acadêmica
Declaro que o presente projeto foi desenvolvido por mim, cumprindo integralmente as exigências técnicas da rubrica do **Projeto A (100 pontos)**. Todas as decisões de arquitetura e implementação foram fundamentadas em Registros de Decisão Arquitetural (ADRs) e estão aptas para defesa técnica ao vivo.
