-- ===================================================================
-- Migration V1: Initial Database Schema (100% English Conventions)
-- Modules: catalog, orders, payment
-- ===================================================================

-- Products Table (Module: catalog)
CREATE TABLE IF NOT EXISTS products (
    id BIGSERIAL PRIMARY KEY,
    sku VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    capacity_liters NUMERIC(5,2) NOT NULL,
    returnable BOOLEAN NOT NULL DEFAULT TRUE,
    product_type VARCHAR(30) NOT NULL,
    unit_price NUMERIC(10,2) NOT NULL,
    stock_full INTEGER NOT NULL DEFAULT 0,
    stock_empty INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0
);

-- Orders Table (Module: orders)
CREATE TABLE IF NOT EXISTS orders (
    id BIGSERIAL PRIMARY KEY,
    order_code VARCHAR(40) NOT NULL UNIQUE,
    customer_name VARCHAR(100) NOT NULL,
    customer_phone VARCHAR(20) NOT NULL,
    modality VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    subtotal NUMERIC(10,2) NOT NULL,
    delivery_fee NUMERIC(10,2) NOT NULL DEFAULT 0.00,
    total_amount NUMERIC(10,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Order Items Table (Module: orders)
CREATE TABLE IF NOT EXISTS order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id),
    quantity INTEGER NOT NULL,
    unit_price NUMERIC(10,2) NOT NULL,
    total_item_amount NUMERIC(10,2) NOT NULL,
    bottles_returned BOOLEAN NOT NULL DEFAULT FALSE
);

-- Payments Table (Module: payment)
CREATE TABLE IF NOT EXISTS payments (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    idempotency_key VARCHAR(80) NOT NULL UNIQUE,
    payment_method VARCHAR(20) NOT NULL,
    paid_amount NUMERIC(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
