-- ==============================================================================
-- Migration V17: Create return_requests Table for Self-Service Returns & Exchanges
-- SareeKart v3.0 Module 1: Customer Returns and Exchanges
-- ==============================================================================

CREATE TABLE IF NOT EXISTS return_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    type VARCHAR(30) NOT NULL DEFAULT 'RETURN',
    reason VARCHAR(50) NOT NULL,
    comments TEXT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    images TEXT NULL,
    refund_amount DECIMAL(10, 2) NULL,
    refund_mode VARCHAR(50) NULL,
    exchange_sku VARCHAR(100) NULL,
    reverse_courier VARCHAR(100) NULL,
    reverse_tracking_number VARCHAR(100) NULL,
    admin_notes TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_return_requests_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE RESTRICT,
    CONSTRAINT fk_return_requests_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT uk_return_requests_order UNIQUE (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Query optimization indexes
CREATE INDEX idx_return_requests_user ON return_requests (user_id);
CREATE INDEX idx_return_requests_order ON return_requests (order_id);
CREATE INDEX idx_return_requests_status ON return_requests (status);
CREATE INDEX idx_return_requests_created_at ON return_requests (created_at);
