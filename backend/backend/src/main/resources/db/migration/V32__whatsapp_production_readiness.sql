-- V32: WhatsApp Production Readiness Migration
-- 1. Regulatory compliance: Add opted_in and opt_in_updated_at to whatsapp_contacts
ALTER TABLE whatsapp_contacts
    ADD COLUMN opted_in BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN opt_in_updated_at TIMESTAMP NULL;

-- 2. Human-in-the-loop escalation: Expand conversations.status ENUM
ALTER TABLE conversations
    MODIFY COLUMN status ENUM('BOT_HANDLING', 'CLOSED', 'OPEN', 'HUMAN_ESCALATION') NOT NULL;
