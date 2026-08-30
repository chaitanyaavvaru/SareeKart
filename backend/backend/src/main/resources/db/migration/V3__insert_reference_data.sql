-- V3: Insert reference data (roles, default admin, categories)

-- Insert roles
INSERT INTO roles (id, name, description) VALUES
    (1, 'ROLE_ADMIN', 'System administrator with full access'),
    (2, 'ROLE_CUSTOMER', 'Registered customer')
ON DUPLICATE KEY UPDATE description = VALUES(description);

-- Insert default admin user (password: admin123 - BCrypt encoded)
-- BCrypt hash for 'admin123' with strength 12: $2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj/RK.PZvO.S
INSERT INTO users (id, email, password, first_name, last_name, phone, enabled, email_verified, created_at, updated_at) VALUES
    (1, 'admin@sareekart.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj/RK.PZvO.S', 'Admin', 'User', '+91-9999999999', TRUE, TRUE, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- Assign admin role to admin user
INSERT INTO user_roles (user_id, role_id) VALUES (1, 1)
ON DUPLICATE KEY UPDATE user_id = user_id;

-- Insert default categories (Indian saree categories)
INSERT INTO categories (id, name, slug, description, image_url, parent_id, sort_order, active) VALUES
    (1, 'Kanchipuram Silk', 'kanchipuram-silk', 'Traditional Kanchipuram silk sarees with pure gold zari', 'https://kankatala.com/cdn/shop/files/1215863175_2.webp?v=1761908736', NULL, 1, TRUE),
    (2, 'Banarasi Silk', 'banarasi-silk', 'Exquisite Banarasi silk sarees with intricate brocade work', 'https://kankatala.com/cdn/shop/files/1214939982_2.jpg?v=1740403250', NULL, 2, TRUE),
    (3, 'Uppada Silk', 'uppada-silk', 'Lightweight Uppada silk sarees from Andhra Pradesh', 'https://kankatala.com/cdn/shop/files/1216039129_1.webp?v=1777711821', NULL, 3, TRUE),
    (4, 'Paithani Silk', 'paithani-silk', 'Maharashtrian Paithani silk with peacock motifs', 'https://kankatala.com/cdn/shop/files/1216423500_1.webp?v=1780133134', NULL, 4, TRUE),
    (5, 'Pochampally Ikat', 'pochampally-ikat', 'Geometric Ikat weaves from Telangana', 'https://kankatala.com/cdn/shop/files/1216423500_1.webp?v=1780133134', NULL, 5, TRUE),
    (6, 'Gadwal Silk', 'gadwal-silk', 'Gadwal silk sarees with cotton body and silk borders', NULL, NULL, 6, TRUE),
    (7, 'Tussar Silk', 'tussar-silk', 'Natural Tussar silk with raw texture', 'https://kankatala.com/cdn/shop/files/1215745847_1.webp?v=1777723358&width=1946', NULL, 7, TRUE),
    (8, 'Organza', 'organza', 'Sheer organza sarees with delicate embroidery', NULL, NULL, 8, TRUE),
    (9, 'Cotton', 'cotton', 'Comfortable cotton sarees for daily wear', 'https://kankatala.com/cdn/shop/files/1216423158_1.webp?v=1780134877', NULL, 9, TRUE),
    (10, 'Linen', 'linen', 'Breathable linen sarees for summer', NULL, NULL, 10, TRUE),
    (11, 'Bridal Sarees', 'bridal-sarees', 'Heavy bridal sarees for weddings', NULL, 1, 1, TRUE),
    (12, 'Festive Wear', 'festive-wear', 'Sarees for festivals and celebrations', NULL, 2, 2, TRUE),
    (13, 'Daily Wear', 'daily-wear', 'Lightweight sarees for everyday use', NULL, 9, 3, TRUE),
    (14, 'Party Wear', 'party-wear', 'Glamorous sarees for parties', NULL, 8, 4, TRUE)
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- Reset auto-increment values
ALTER TABLE roles AUTO_INCREMENT = 100;
ALTER TABLE users AUTO_INCREMENT = 100;
ALTER TABLE categories AUTO_INCREMENT = 100;