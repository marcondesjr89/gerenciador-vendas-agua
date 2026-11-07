-- ===================================================================
-- Migration V2: 100k Synthetic Orders Volume & Composite Indexing
-- Rastreabilidade: RF-005, RNF-M1.4, RNF-M4.4
-- ===================================================================

-- 1. Ensure baseline catalog products exist for foreign key integrity
INSERT INTO products (id, sku, name, capacity_liters, returnable, product_type, unit_price, stock_full, stock_empty, active, version)
VALUES
    (1, 'GAL-20L-COMP', 'Galao 20L Retornavel (Completo)', 20.00, true, 'FULL_WATER', 30.00, 50000, 10000, true, 0),
    (2, 'GAL-20L-REFIL', 'Refil Galao 20L (Troca de Casco)', 20.00, true, 'REFILL', 14.00, 50000, 20000, true, 0),
    (3, 'VAS-20L-VAZIO', 'Vasilhame Vazio 20L (Casco)', 20.00, true, 'EMPTY_BOTTLE', 25.00, 0, 15000, true, 0),
    (4, 'GAR-500ML-PACK', 'Fardo 12x Garrafas 500ml', 6.00, false, 'DISPOSABLE', 18.00, 20000, 0, true, 0)
ON CONFLICT (id) DO NOTHING;

-- 2. Populate 100,000 realistic synthetic orders using generate_series
INSERT INTO orders (
    id,
    order_code,
    customer_name,
    customer_phone,
    modality,
    status,
    payment_method,
    subtotal,
    delivery_fee,
    total_amount,
    created_at
)
SELECT
    s,
    'ORD-SYNTH-' || LPAD(s::text, 6, '0'),
    'Cliente Sintetico ' || (s % 5000),
    '119' || LPAD((10000000 + (s % 5000))::text, 8, '0'),
    CASE WHEN s % 3 = 0 THEN 'PICKUP' ELSE 'DELIVERY' END,
    CASE
        WHEN s % 20 = 0 THEN 'CANCELLED'
        WHEN s % 10 = 0 THEN 'IN_TRANSIT'
        WHEN s % 5 = 0 THEN 'CONFIRMED'
        ELSE 'DELIVERED'
    END,
    CASE
        WHEN s % 4 = 0 THEN 'PIX'
        WHEN s % 4 = 1 THEN 'CASH'
        WHEN s % 4 = 2 THEN 'CREDIT_CARD'
        ELSE 'DEBIT_CARD'
    END,
    CASE WHEN s % 2 = 0 THEN 28.00 ELSE 42.00 END,
    CASE WHEN s % 3 = 0 OR s % 5 = 0 THEN 0.00 ELSE 5.00 END,
    (CASE WHEN s % 2 = 0 THEN 28.00 ELSE 42.00 END) + (CASE WHEN s % 3 = 0 OR s % 5 = 0 THEN 0.00 ELSE 5.00 END),
    CURRENT_TIMESTAMP - (s * INTERVAL '30 seconds')
FROM generate_series(1, 100000) AS s
ON CONFLICT (order_code) DO NOTHING;

-- 3. Populate corresponding order items for the synthetic orders
INSERT INTO order_items (
    id,
    order_id,
    product_id,
    quantity,
    unit_price,
    total_item_amount,
    bottles_returned
)
SELECT
    s,
    s,
    CASE WHEN s % 2 = 0 THEN 2 ELSE 1 END,
    CASE WHEN s % 3 = 0 THEN 2 ELSE 1 END,
    CASE WHEN s % 2 = 0 THEN 14.00 ELSE 30.00 END,
    (CASE WHEN s % 2 = 0 THEN 14.00 ELSE 30.00 END) * (CASE WHEN s % 3 = 0 THEN 2 ELSE 1 END),
    CASE WHEN s % 2 = 0 THEN true ELSE false END
FROM generate_series(1, 100000) AS s
ON CONFLICT (id) DO NOTHING;

-- 4. Sync sequences to prevent ID collisions on subsequent runtime inserts
SELECT setval('orders_id_seq', (SELECT COALESCE(MAX(id), 1) FROM orders), true);
SELECT setval('order_items_id_seq', (SELECT COALESCE(MAX(id), 1) FROM order_items), true);
SELECT setval('products_id_seq', (SELECT COALESCE(MAX(id), 1) FROM products), true);

-- 5. High-Performance Composite & Foreign Key Indexes
-- Composite index for fast paginated queries by status and date (RNF-M1.4 / RF-005)
CREATE INDEX IF NOT EXISTS idx_orders_status_created_at ON orders(status, created_at DESC);

-- Secondary index for customer phone lookups
CREATE INDEX IF NOT EXISTS idx_orders_customer_phone ON orders(customer_phone);

-- Index for order items foreign key join performance
CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items(order_id);
