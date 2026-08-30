-- V13: Refund reconciliation & payment-operations hardening.

-- Structured reason code (nullable: historical free-text-only rows stay valid;
-- NULL is treated as OTHER by readers).
ALTER TABLE refunds
    ADD COLUMN reason_code VARCHAR(40) NULL AFTER reason;

-- Reconciliation attempt metadata + bounded backoff bookkeeping.
ALTER TABLE refunds
    ADD COLUMN last_attempt_at TIMESTAMP NULL,
    ADD COLUMN attempt_count INT NOT NULL DEFAULT 0,
    ADD COLUMN next_retry_at TIMESTAMP NULL;

CREATE INDEX idx_refunds_stale_pending ON refunds (status, next_retry_at);

-- Durable operational anomalies (structured codes, no sensitive payload).
CREATE TABLE reconciliation_anomalies (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(60) NOT NULL,
    severity ENUM('INFO', 'WARNING', 'CRITICAL') NOT NULL DEFAULT 'WARNING',
    order_id BIGINT NULL,
    payment_id BIGINT NULL,
    refund_id BIGINT NULL,
    provider_refund_id VARCHAR(100) NULL,
    provider_payment_id VARCHAR(100) NULL,
    detail VARCHAR(500) NOT NULL,
    resolved BOOLEAN NOT NULL DEFAULT FALSE,
    resolved_by VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_anomalies_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE SET NULL,
    CONSTRAINT fk_anomalies_payment FOREIGN KEY (payment_id) REFERENCES payments (id) ON DELETE SET NULL,
    CONSTRAINT fk_anomalies_refund FOREIGN KEY (refund_id) REFERENCES refunds (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_anomalies_code_resolved ON reconciliation_anomalies (code, resolved);
CREATE INDEX idx_anomalies_refund ON reconciliation_anomalies (refund_id);