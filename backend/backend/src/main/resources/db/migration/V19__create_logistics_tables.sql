-- ==============================================================================
-- Migration V19: Create pincode_overrides Table for Courier Logistics
-- SareeKart v3.0 Module 3: Live Pincode Logistics & Courier Serviceability
-- ==============================================================================

CREATE TABLE IF NOT EXISTS pincode_overrides (
    id BIGINT NOT NULL AUTO_INCREMENT,
    pincode VARCHAR(6) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    zone VARCHAR(30) NOT NULL DEFAULT 'TIER_1',
    is_serviceable BOOLEAN NOT NULL DEFAULT TRUE,
    is_cod_available BOOLEAN NOT NULL DEFAULT TRUE,
    courier_partner VARCHAR(100) NOT NULL DEFAULT 'Blue Dart Apex Air',
    transit_days INT NOT NULL DEFAULT 3,
    notes TEXT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_pincode_overrides_pincode UNIQUE (pincode)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_pincode_overrides_pin ON pincode_overrides (pincode);
CREATE INDEX idx_pincode_overrides_zone ON pincode_overrides (zone);
