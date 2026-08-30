-- V8: Razorpay payment support
-- Adds UNIQUE constraint on provider_order_id for idempotency,
-- webhook_events table for idempotent webhook processing,
-- payment status transition tracking columns.
--
-- ALSO aligns payments.status with the Java PaymentStatus enum
-- (V1 shipped 'SUCCESS'; the domain model uses 'PAID'). Latent until now
-- because no code path ever wrote a non-PENDING payment status.

ALTER TABLE payments MODIFY COLUMN status
    ENUM('PENDING', 'PAID', 'FAILED', 'REFUNDED', 'PARTIALLY_REFUNDED')
    NOT NULL DEFAULT 'PENDING';

-- Ensure provider_order_id is unique for idempotent payment verification
-- MySQL allows multiple NULLs, so only non-null values are constrained
ALTER TABLE payments ADD CONSTRAINT uk_payments_provider_order_id UNIQUE (provider_order_id);

-- Track payment status transitions for audit & debugging
ALTER TABLE payments ADD COLUMN status_transition_history JSON;

-- Webhook events table for idempotent webhook processing
CREATE TABLE webhook_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    payload JSON NOT NULL,
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    payment_id BIGINT,
    error_message VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_webhook_events_event_id UNIQUE (event_id),
    CONSTRAINT fk_webhook_events_payment FOREIGN KEY (payment_id) REFERENCES payments (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_webhook_events_payment_id ON webhook_events (payment_id);
CREATE INDEX idx_webhook_events_processed ON webhook_events (processed);