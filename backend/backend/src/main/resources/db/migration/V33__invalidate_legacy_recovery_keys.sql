-- ==============================================================================
-- SareeKart Migration V33: Invalidate and Clear Legacy Recovery Keys (SEC-01 / SEC-02)
-- Clears legacy plaintext and low-entropy static demo keys from persistent storage.
-- Users whose previous recovery keys are cleared must provision new high-entropy
-- 128-bit keys via the authorized POST /api/auth/generate-recovery-key workflow.
-- Data-only migration: does not alter schema, tables, or column definitions.
-- ==============================================================================

UPDATE `users`
SET `recovery_key` = NULL
WHERE `recovery_key` IS NOT NULL;
