-- V10: Coupons & discounts
-- Pricing snapshot: orders.coupon_code joins the existing subtotal /
-- discount_amount / tax_amount / shipping_amount / total_amount columns to
-- form a complete immutable record — no duplicated pricing data.

CREATE TABLE coupons (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    discount_type ENUM('PERCENTAGE', 'FIXED_AMOUNT') NOT NULL,
    discount_value DECIMAL(10,2) NOT NULL,
    minimum_order_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    maximum_discount_amount DECIMAL(10,2) NULL,
    valid_from TIMESTAMP NULL,
    valid_until TIMESTAMP NULL,
    total_usage_limit INT NULL,
    per_user_usage_limit INT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_coupons_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_coupons_active ON coupons (active);

-- One redemption row per order = reservation ledger. The UNIQUE(order_id)
-- makes reservation idempotent and race-proof at the storage layer; quota
-- checks run under a pessimistic lock on the coupon row.
CREATE TABLE coupon_redemptions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    coupon_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    discount_applied DECIMAL(10,2) NOT NULL,
    redeemed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_coupon_redemptions_order UNIQUE (order_id),
    CONSTRAINT fk_redemptions_coupon FOREIGN KEY (coupon_id) REFERENCES coupons (id) ON DELETE CASCADE,
    CONSTRAINT fk_redemptions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_redemptions_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_redemptions_coupon_user ON coupon_redemptions (coupon_id, user_id);
CREATE INDEX idx_redemptions_coupon_redeemed ON coupon_redemptions (coupon_id, redeemed);

-- Historical snapshot column
ALTER TABLE orders ADD COLUMN coupon_code VARCHAR(50) NULL AFTER discount_amount;