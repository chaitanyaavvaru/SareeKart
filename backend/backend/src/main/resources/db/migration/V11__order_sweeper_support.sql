-- V11: PENDING order sweeper support.
-- cancel_reason distinguishes user cancellations from system expiry;
-- composite index backs the stale-candidate scan.

ALTER TABLE orders ADD COLUMN cancel_reason VARCHAR(50) NULL AFTER cancelled_at;

CREATE INDEX idx_orders_stale_candidates
    ON orders (payment_method, status, payment_status, created_at);

-- Seed reason for historical rows cancelled by users.
UPDATE orders SET cancel_reason = 'USER' WHERE status = 'CANCELLED' AND cancel_reason IS NULL;