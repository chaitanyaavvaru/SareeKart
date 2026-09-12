-- V21: Create WhatsApp Notification & Dispatch Logs Table
CREATE TABLE IF NOT EXISTS whatsapp_notification_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NULL,
    return_request_id BIGINT NULL,
    user_id BIGINT NOT NULL,
    recipient_name VARCHAR(100) NOT NULL,
    recipient_phone VARCHAR(30) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    template_name VARCHAR(100) NOT NULL,
    message_content TEXT NOT NULL,
    tracking_number VARCHAR(100) NULL,
    courier_partner VARCHAR(100) NULL,
    tracking_url VARCHAR(500) NULL,
    delivery_status VARCHAR(30) NOT NULL DEFAULT 'SENT',
    simulated BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_wa_order_id (order_id),
    INDEX idx_wa_user_id (user_id),
    INDEX idx_wa_event_type (event_type),
    INDEX idx_wa_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Safely add whatsapp_opt_in to users if not present
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'whatsapp_opt_in');
SET @query := IF(@col_exists = 0, 'ALTER TABLE users ADD COLUMN whatsapp_opt_in BOOLEAN NOT NULL DEFAULT TRUE', 'SELECT 1');
PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
