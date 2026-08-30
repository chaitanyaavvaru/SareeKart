-- V2: Add indexes and additional constraints for performance

-- Users indexes
CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_enabled ON users (enabled);
CREATE INDEX idx_users_created_at ON users (created_at);

-- Categories indexes
CREATE INDEX idx_categories_parent ON categories (parent_id);
CREATE INDEX idx_categories_active ON categories (active);
CREATE INDEX idx_categories_slug ON categories (slug);

-- Products indexes
CREATE INDEX idx_products_category ON products (category_id);
CREATE INDEX idx_products_active ON products (active);
CREATE INDEX idx_products_featured ON products (featured);
CREATE INDEX idx_products_slug ON products (slug);
CREATE INDEX idx_products_fabric ON products (fabric);
CREATE INDEX idx_products_occasion ON products (occasion);
CREATE INDEX idx_products_price ON products (base_price);
CREATE INDEX idx_products_created_at ON products (created_at);
CREATE FULLTEXT INDEX idx_products_search ON products (name, description, short_description);

-- Product Images indexes
CREATE INDEX idx_product_images_product ON product_images (product_id);
CREATE INDEX idx_product_images_primary ON product_images (product_id, is_primary);

-- Product Variants indexes
CREATE INDEX idx_product_variants_product ON product_variants (product_id);
CREATE INDEX idx_product_variants_active ON product_variants (active);
CREATE INDEX idx_product_variants_sku ON product_variants (sku);

-- Addresses indexes
CREATE INDEX idx_addresses_user ON addresses (user_id);
CREATE INDEX idx_addresses_user_default ON addresses (user_id, is_default);

-- Cart Items indexes
CREATE INDEX idx_cart_items_user ON cart_items (user_id);

-- Wishlist indexes
CREATE INDEX idx_wishlist_user ON wishlist (user_id);

-- Orders indexes
CREATE INDEX idx_orders_user ON orders (user_id);
CREATE INDEX idx_orders_status ON orders (status);
CREATE INDEX idx_orders_created_at ON orders (created_at);
CREATE INDEX idx_orders_order_number ON orders (order_number);

-- Order Items indexes
CREATE INDEX idx_order_items_order ON order_items (order_id);
CREATE INDEX idx_order_items_product ON order_items (product_id);

-- Payments indexes
CREATE INDEX idx_payments_order ON payments (order_id);
CREATE INDEX idx_payments_status ON payments (status);
CREATE INDEX idx_payments_provider_order ON payments (provider_order_id);

-- Reviews indexes
CREATE INDEX idx_reviews_product ON reviews (product_id);
CREATE INDEX idx_reviews_user ON reviews (user_id);
CREATE INDEX idx_reviews_approved ON reviews (approved);