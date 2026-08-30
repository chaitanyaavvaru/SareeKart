-- V12: Refund processing.
--
-- Refund status lives HERE (per refund row), deliberately separate from
-- payments.status / orders.payment_status (which become aggregates).

CREATE TABLE refunds (
    id BIGINT NOT NULL AUTO_INCREMENT,
    payment_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    provider_refund_id VARCHAR(100) NULL,
    amount DECIMAL(10,2) NOT NULL,
    status ENUM('PENDING', 'SUCCESS', 'FAILED') NOT NULL DEFAULT 'PENDING',
    reason VARCHAR(255),
    initiated_by VARCHAR(255) NOT NULL,
    error_message VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    -- Gateway idempotency: one local row per gateway refund, ever.
    CONSTRAINT uk_refunds_provider_refund_id UNIQUE (provider_refund_id),
    CONSTRAINT fk_refunds_payment FOREIGN KEY (payment_id) REFERENCES payments (id) ON DELETE RESTRICT,
    CONSTRAINT fk_refunds_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_refunds_payment ON refunds (payment_id);
CREATE INDEX idx_refunds_status ON refunds (status);
CREATE INDEX idx_refunds_order ON refunds (order_id);

-- Durable exactly-once marker for the automatic pre-fulfillment restock.
ALTER TABLE orders ADD COLUMN inventory_restocked BOOLEAN NOT NULL DEFAULT FALSE AFTER cancel_reason;