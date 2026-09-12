-- ==============================================================================
-- Migration: V26__orders_and_inventory_hardening.sql
-- Description: Phase 5 Orders & Inventory Concurrency, Snapshots & Idempotency
-- ==============================================================================

-- 1. Snapshot columns on order_items
ALTER TABLE order_items 
    ADD COLUMN product_name VARCHAR(255) NOT NULL DEFAULT 'Saree Product' AFTER product_id,
    ADD COLUMN product_image VARCHAR(1000) NULL AFTER product_name;

-- Backfill product_name from products
UPDATE order_items oi 
JOIN products p ON oi.product_id = p.id 
SET oi.product_name = p.name;

-- Backfill product_image from verified primary product_images (if available)
UPDATE order_items oi 
SET oi.product_image = (
    SELECT pi.image_url 
    FROM product_images pi 
    WHERE pi.product_id = oi.product_id 
    ORDER BY pi.image_order ASC 
    LIMIT 1
);

-- 2. Quantity sanity constraint on order_items
ALTER TABLE order_items 
    ADD CONSTRAINT chk_order_items_positive_quantity CHECK (quantity > 0);

-- 3. Idempotency Key & unique constraint on orders
ALTER TABLE orders 
    ADD COLUMN idempotency_key VARCHAR(64) NULL AFTER tracking_number;

CREATE UNIQUE INDEX uq_orders_user_idempotency 
    ON orders(user_id, idempotency_key);

-- 4. High-performance index access for user order history & status queries
CREATE INDEX idx_orders_user_created 
    ON orders(user_id, created_at DESC);

CREATE INDEX idx_orders_status_created 
    ON orders(status, created_at DESC);

-- 5. Non-negative stock constraint on products
ALTER TABLE products 
    ADD CONSTRAINT chk_products_stock_non_negative CHECK (stock_quantity >= 0);
