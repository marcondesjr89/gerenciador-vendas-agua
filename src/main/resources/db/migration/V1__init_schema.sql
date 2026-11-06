-- ===================================================================
-- Migration V1: Estrutura Inicial do Banco de Dados
-- Modulos: catalog, orders, payment
-- ===================================================================

-- Tabela de Produtos (Modulo: catalog)
CREATE TABLE IF NOT EXISTS produtos (
    id BIGSERIAL PRIMARY KEY,
    sku VARCHAR(40) NOT NULL UNIQUE,
    nome VARCHAR(120) NOT NULL,
    capacidade_litros NUMERIC(5,2) NOT NULL,
    retornavel BOOLEAN NOT NULL DEFAULT TRUE,
    tipo_produto VARCHAR(30) NOT NULL,
    preco_venda NUMERIC(10,2) NOT NULL,
    estoque_cheios INTEGER NOT NULL DEFAULT 0,
    estoque_vazios INTEGER NOT NULL DEFAULT 0,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    versao BIGINT NOT NULL DEFAULT 0
);

-- Tabela de Pedidos (Modulo: orders)
CREATE TABLE IF NOT EXISTS pedidos (
    id BIGSERIAL PRIMARY KEY,
    codigo_pedido VARCHAR(40) NOT NULL UNIQUE,
    cliente_nome VARCHAR(100) NOT NULL,
    cliente_telefone VARCHAR(20) NOT NULL,
    modalidade VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    forma_pagamento VARCHAR(20) NOT NULL,
    subtotal NUMERIC(10,2) NOT NULL,
    taxa_entrega NUMERIC(10,2) NOT NULL DEFAULT 0.00,
    valor_total NUMERIC(10,2) NOT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Tabela de Itens de Pedido (Modulo: orders)
CREATE TABLE IF NOT EXISTS itens_pedido (
    id BIGSERIAL PRIMARY KEY,
    pedido_id BIGINT NOT NULL REFERENCES pedidos(id) ON DELETE CASCADE,
    produto_id BIGINT NOT NULL REFERENCES produtos(id),
    quantidade INTEGER NOT NULL,
    preco_unitario NUMERIC(10,2) NOT NULL,
    valor_total_item NUMERIC(10,2) NOT NULL,
    recolheu_vasilhame BOOLEAN NOT NULL DEFAULT FALSE
);

-- Tabela de Pagamentos (Modulo: payment)
CREATE TABLE IF NOT EXISTS pagamentos (
    id BIGSERIAL PRIMARY KEY,
    pedido_id BIGINT NOT NULL REFERENCES pedidos(id) ON DELETE CASCADE,
    idempotency_key VARCHAR(80) NOT NULL UNIQUE,
    forma_pagamento VARCHAR(20) NOT NULL,
    valor_pago NUMERIC(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    processado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
