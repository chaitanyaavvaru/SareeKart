-- ==============================================================================
-- Migration V20: Create ai_style_consultations Table for AI Stylist & Drape Matcher
-- SareeKart v3.0 Module 4: AI Luxury Saree Stylist & Visual Drape Concierge
-- ==============================================================================

CREATE TABLE IF NOT EXISTS ai_style_consultations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NULL,
    product_id BIGINT NULL,
    saree_name VARCHAR(255) NOT NULL,
    fabric VARCHAR(100) NULL,
    primary_color VARCHAR(100) NULL,
    occasion VARCHAR(100) NULL,
    chosen_look_title VARCHAR(255) NULL,
    contrast_color VARCHAR(100) NULL,
    blouse_style VARCHAR(100) NULL,
    jewelry_recommendation VARCHAR(255) NULL,
    converted_to_tailoring BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_ai_style_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_ai_style_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_ai_style_user ON ai_style_consultations (user_id);
CREATE INDEX idx_ai_style_product ON ai_style_consultations (product_id);
CREATE INDEX idx_ai_style_occasion ON ai_style_consultations (occasion);
CREATE INDEX idx_ai_style_converted ON ai_style_consultations (converted_to_tailoring);
CREATE INDEX idx_ai_style_created ON ai_style_consultations (created_at);
