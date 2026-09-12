-- ====================================================================
-- SareeKart Migration V24: Normalize Categories & Product Attributes
-- Phase 2: Category Hierarchy, Fabrics, Occasions, and Colors
-- ====================================================================

-- 1. Upgrade categories table with slug, hierarchy, display order, and active toggle
ALTER TABLE categories
    ADD COLUMN slug VARCHAR(150) UNIQUE AFTER name,
    ADD COLUMN parent_id BIGINT NULL AFTER image_url,
    ADD COLUMN display_order INT DEFAULT 0 AFTER parent_id,
    ADD COLUMN active BIT(1) DEFAULT 1 NOT NULL AFTER display_order,
    ADD CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES categories(id);

-- Set unique, readable slugs for the 8 baseline categories
UPDATE categories SET slug = 'silk-sarees' WHERE id = 1;
UPDATE categories SET slug = 'cotton-sarees' WHERE id = 2;
UPDATE categories SET slug = 'chiffon-sarees' WHERE id = 3;
UPDATE categories SET slug = 'georgette-sarees' WHERE id = 4;
UPDATE categories SET slug = 'banarasi-sarees' WHERE id = 5;
UPDATE categories SET slug = 'kanchipuram-sarees' WHERE id = 6;
UPDATE categories SET slug = 'designer-sarees' WHERE id = 7;
UPDATE categories SET slug = 'bridal-sarees' WHERE id = 8;

-- 2. Create fabrics table
CREATE TABLE IF NOT EXISTS fabrics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    slug VARCHAR(120) NOT NULL UNIQUE,
    description TEXT,
    care_instructions VARCHAR(255),
    display_order INT DEFAULT 0,
    active BIT(1) DEFAULT 1 NOT NULL,
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Seed known distinct fabrics preserving all existing catalog materials
INSERT INTO fabrics (name, slug, care_instructions, display_order) VALUES
('Silk', 'silk', 'Professional petrol dry clean only. Wrap in breathable cotton or muslin.', 1),
('Cotton', 'cotton', 'Gentle hand wash in cold water or mild dry clean. Starch if desired.', 2),
('Chiffon', 'chiffon', 'Mild dry clean. Do not wring, hang dry in shade.', 3),
('Georgette', 'georgette', 'Professional dry clean. Steam iron on reverse.', 4),
('Tussar Silk', 'tussar-silk', 'Gentle dry clean. Store away from direct moisture.', 5),
('Patola Silk', 'patola-silk', 'Handloom dry clean only. Heirloom preservation.', 6),
('Katan Silk', 'katan-silk', 'Dry clean only. Refold quarterly to protect zari creases.', 7),
('Linen', 'linen', 'Gentle hand wash or dry clean. Iron while slightly damp.', 8),
('Organza', 'organza', 'Dry clean only. Hang on padded hangers to avoid crushing.', 9),
('Chanderi Silk', 'chanderi-silk', 'Dry clean recommended. Low iron setting.', 10);

-- 3. Create occasions table
CREATE TABLE IF NOT EXISTS occasions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    slug VARCHAR(120) NOT NULL UNIQUE,
    description VARCHAR(255),
    display_order INT DEFAULT 0,
    active BIT(1) DEFAULT 1 NOT NULL,
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Seed distinct occasions preserving all current catalog usage
INSERT INTO occasions (name, slug, description, display_order) VALUES
('Wedding', 'wedding', 'Grand ceremonies, muhurtham, and traditional marriage rituals', 1),
('Bridal', 'bridal', 'Sacred bridal trousseau and bride-specific ceremonies', 2),
('Festive', 'festive', 'Festive celebrations, Diwali, Pongal, and Dussehra celebrations', 3),
('Festival', 'festival', 'Traditional cultural and temple festivals', 4),
('Casual', 'casual', 'Comfortable outings, weekend wear, and informal gatherings', 5),
('Daily', 'daily', 'Daily office wear and breathable everyday comfort', 6),
('Party', 'party', 'Cocktails, sangeet celebrations, and evening parties', 7),
('Formal', 'formal', 'Conferences, academic convocations, and executive ceremonies', 8);

-- 4. Create colors table
CREATE TABLE IF NOT EXISTS colors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(80) NOT NULL UNIQUE,
    slug VARCHAR(100) NOT NULL UNIQUE,
    family VARCHAR(50) NOT NULL,
    hex_code VARCHAR(10) NOT NULL,
    display_order INT DEFAULT 0,
    active BIT(1) DEFAULT 1 NOT NULL,
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Seed all 19 existing catalog colors and nuances without loss
INSERT INTO colors (name, slug, family, hex_code, display_order) VALUES
('Red', 'red', 'Red', '#FF0000', 1),
('Maroon', 'maroon', 'Red', '#800000', 2),
('Crimson', 'crimson', 'Red', '#DC143C', 3),
('Crimson Red', 'crimson-red', 'Red', '#990000', 4),
('Ruby Red', 'ruby-red', 'Red', '#C70039', 5),
('Pink', 'pink', 'Pink', '#FFC0CB', 6),
('baby pink', 'baby-pink', 'Pink', '#F4C2C2', 7),
('Ruby Pink', 'ruby-pink', 'Pink', '#E0115F', 8),
('Rose', 'rose', 'Pink', '#FF007F', 9),
('White', 'white', 'White', '#FFFFFF', 10),
('Ivory', 'ivory', 'White', '#FFFFF0', 11),
('Green', 'green', 'Green', '#008000', 12),
('Emerald Green', 'emerald-green', 'Green', '#50C878', 13),
('Navy Blue', 'navy-blue', 'Blue', '#000080', 14),
('Peacock Blue', 'peacock-blue', 'Blue', '#005F73', 15),
('Yellow', 'yellow', 'Yellow', '#FFFF00', 16),
('Gold', 'gold', 'Gold', '#FFD700', 17),
('Royal Gold', 'royal-gold', 'Gold', '#D4AF37', 18),
('Honey Gold', 'honey-gold', 'Gold', '#E5B80B', 19),
('Beige', 'beige', 'Neutral', '#F5F5DC', 20),
('Champagne Silver', 'champagne-silver', 'Metallic', '#E5E4E2', 21),
('Terracotta Earth', 'terracotta-earth', 'Orange', '#E2725B', 22),
('Black', 'black', 'Black', '#000000', 23);

-- 5. Add foreign key columns to products while preserving legacy strings
ALTER TABLE products
    ADD COLUMN fabric_id BIGINT NULL AFTER fabric,
    ADD COLUMN occasion_id BIGINT NULL AFTER occasion,
    ADD COLUMN color_id BIGINT NULL AFTER color,
    ADD CONSTRAINT fk_products_fabric FOREIGN KEY (fabric_id) REFERENCES fabrics(id),
    ADD CONSTRAINT fk_products_occasion FOREIGN KEY (occasion_id) REFERENCES occasions(id),
    ADD CONSTRAINT fk_products_color FOREIGN KEY (color_id) REFERENCES colors(id);

-- 6. Backfill existing product foreign keys using exact case-insensitive matches
UPDATE products p JOIN fabrics f ON LOWER(TRIM(p.fabric)) = LOWER(TRIM(f.name)) SET p.fabric_id = f.id;
UPDATE products p JOIN occasions o ON LOWER(TRIM(p.occasion)) = LOWER(TRIM(o.name)) SET p.occasion_id = o.id;
UPDATE products p JOIN colors c ON LOWER(TRIM(p.color)) = LOWER(TRIM(c.name)) SET p.color_id = c.id;
