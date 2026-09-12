-- V22: Create AI Visual Search Queries Table
CREATE TABLE IF NOT EXISTS visual_search_queries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NULL,
    source VARCHAR(50) NOT NULL,
    extracted_primary_color VARCHAR(50) NOT NULL,
    extracted_secondary_color VARCHAR(50) NULL,
    extracted_weave_type VARCHAR(100) NULL,
    top_matched_product_id BIGINT NULL,
    confidence_score DECIMAL(5, 2) NOT NULL DEFAULT 95.00,
    execution_time_ms INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_vs_user_id (user_id),
    INDEX idx_vs_created_at (created_at),
    INDEX idx_vs_top_product (top_matched_product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
