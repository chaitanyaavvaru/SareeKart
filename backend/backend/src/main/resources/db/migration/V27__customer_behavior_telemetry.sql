-- ==============================================================================
-- Migration: V27__customer_behavior_telemetry.sql
-- Description: Phase 6 Customer Behavioral Telemetry & Event Stream
-- ==============================================================================

CREATE TABLE IF NOT EXISTS customer_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_event_id VARCHAR(64) NOT NULL,
    session_id VARCHAR(64) NOT NULL,
    user_id BIGINT NULL,
    event_type VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NULL,
    entity_id BIGINT NULL,
    metadata JSON NULL,
    ip_address VARCHAR(45) NULL,
    user_agent VARCHAR(255) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_customer_events_client_event_id UNIQUE (client_event_id),
    CONSTRAINT fk_customer_events_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- High-performance query indexes
CREATE INDEX idx_customer_events_session 
    ON customer_events (session_id, created_at DESC);

CREATE INDEX idx_customer_events_user_type 
    ON customer_events (user_id, event_type, created_at DESC);

CREATE INDEX idx_customer_events_type_created 
    ON customer_events (event_type, created_at DESC);

CREATE INDEX idx_customer_events_entity 
    ON customer_events (entity_type, entity_id);

CREATE INDEX idx_customer_events_created 
    ON customer_events (created_at DESC);
