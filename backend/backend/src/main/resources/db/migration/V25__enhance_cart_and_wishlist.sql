-- =========================================================================
-- Migration V25: Enhance Cart and Wishlist Integrity & Concurrency
-- SareeKart Phase 4 — Deterministic Deduplication, Constraints & Foreign Keys
-- =========================================================================

-- -------------------------------------------------------------------------
-- 1. Deterministic Deduplication of cart_items
-- Deduplication Rule:
-- For any duplicate (cart_id, product_id) groups:
--   a) Aggregate total quantity: Q_total = SUM(quantity)
--   b) Enforce max SKU quantity cap: Q_merged = LEAST(Q_total, 10)
--   c) Keep the primary record (lowest id) and update its quantity to Q_merged
--   d) Delete all secondary duplicate records (id != min(id))
-- -------------------------------------------------------------------------

CREATE TEMPORARY TABLE IF NOT EXISTS temp_cart_item_dups AS
SELECT 
    MIN(id) AS keep_id,
    cart_id,
    product_id,
    LEAST(SUM(quantity), 10) AS merged_quantity
FROM cart_items
GROUP BY cart_id, product_id
HAVING COUNT(*) > 1;

UPDATE cart_items c
JOIN temp_cart_item_dups d ON c.id = d.keep_id
SET c.quantity = d.merged_quantity;

DELETE c FROM cart_items c
JOIN temp_cart_item_dups d 
  ON c.cart_id = d.cart_id 
 AND c.product_id = d.product_id 
 AND c.id <> d.keep_id;

DROP TEMPORARY TABLE IF EXISTS temp_cart_item_dups;

-- -------------------------------------------------------------------------
-- 2. Add Unique Constraint on cart_items (cart_id, product_id)
-- Guarantees atomic single-row entry per product per cart
-- -------------------------------------------------------------------------
ALTER TABLE cart_items
ADD CONSTRAINT uq_cart_items_cart_product UNIQUE (cart_id, product_id);

-- -------------------------------------------------------------------------
-- 3. Enhance wishlists Table:
--   a) Add created_at timestamp column
--   b) Purge any existing orphaned records referencing non-existent users/products
--   c) Add foreign keys to users(id) and products(id) with ON DELETE CASCADE
--   d) Add index for user's recent wishlist queries
-- -------------------------------------------------------------------------

ALTER TABLE wishlists
ADD COLUMN created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6);

UPDATE wishlists SET created_at = NOW(6) WHERE created_at IS NULL;

-- Remove any orphaned rows before adding foreign keys
DELETE w FROM wishlists w
LEFT JOIN users u ON w.user_id = u.id
WHERE u.id IS NULL;

DELETE w FROM wishlists w
LEFT JOIN products p ON w.product_id = p.id
WHERE p.id IS NULL;

ALTER TABLE wishlists
ADD CONSTRAINT fk_wishlists_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
ADD CONSTRAINT fk_wishlists_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE;

CREATE INDEX idx_wishlists_user_created ON wishlists(user_id, created_at DESC);
