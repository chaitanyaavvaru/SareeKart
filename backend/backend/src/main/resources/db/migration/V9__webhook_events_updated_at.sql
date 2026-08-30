-- V9: webhook_events was created without the updated_at audit column required
-- by BaseEntity (same class of drift fixed for other tables in V5).

ALTER TABLE webhook_events
    ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER created_at;