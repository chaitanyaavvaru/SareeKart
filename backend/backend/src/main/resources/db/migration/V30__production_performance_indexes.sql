-- ============================================================================
-- V30: Production Performance Index Optimization
-- Accelerated query execution for catalog browsing, inventory, and reviews
-- ============================================================================

-- 1. Catalog performance: speed up active catalog scans and price-filtered searches
CREATE INDEX idx_products_active_price ON products (active, price);
CREATE INDEX idx_products_active_category ON products (active, category_id);

-- 2. Inventory performance: speed up stock lookups by product ID
CREATE INDEX idx_inventory_product ON inventory_items (product_id);

-- 3. Review performance: speed up product rating & customer review lookups
CREATE INDEX idx_reviews_product ON reviews (product_id);
