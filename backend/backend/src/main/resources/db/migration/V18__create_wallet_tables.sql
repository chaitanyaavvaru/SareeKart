-- ==============================================================================
-- Migration V18: Create wallets and wallet_transactions Tables
-- SareeKart v3.0 Module 2: Customer Loyalty Points, Store Credit Wallet & Rewards
-- ==============================================================================

CREATE TABLE IF NOT EXISTS wallets (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    balance DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    loyalty_points INT NOT NULL DEFAULT 0,
    tier VARCHAR(30) NOT NULL DEFAULT 'SILVER',
    lifetime_spent DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_wallets_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT uk_wallets_user UNIQUE (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS wallet_transactions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    wallet_id BIGINT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    points INT NOT NULL DEFAULT 0,
    type VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    reference_id BIGINT NULL,
    reference_type VARCHAR(50) NULL,
    balance_after DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_wallet_transactions_wallet FOREIGN KEY (wallet_id) REFERENCES wallets (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Query optimization indexes
CREATE INDEX idx_wallets_user ON wallets (user_id);
CREATE INDEX idx_wallets_tier ON wallets (tier);
CREATE INDEX idx_wallet_tx_wallet ON wallet_transactions (wallet_id);
CREATE INDEX idx_wallet_tx_type ON wallet_transactions (type);
CREATE INDEX idx_wallet_tx_created ON wallet_transactions (created_at);
