-- ==============================================================================
-- Migration: V28__graph_sync_support.sql
-- Description: Phase 7 Neo4j Knowledge Graph Synchronization & Dead-Letter Queue
-- ==============================================================================

CREATE TABLE IF NOT EXISTS graph_sync_failures (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id BIGINT NULL,
    client_event_id VARCHAR(64) NULL,
    event_type VARCHAR(50) NOT NULL,
    payload JSON NULL,
    error_message TEXT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    resolved_at DATETIME(6) NULL,
    INDEX idx_graph_sync_unresolved (resolved_at, retry_count),
    INDEX idx_graph_sync_created (created_at DESC)
);
