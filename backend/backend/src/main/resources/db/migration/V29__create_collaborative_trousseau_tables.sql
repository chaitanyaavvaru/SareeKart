-- ==============================================================================
-- Migration: V29__create_collaborative_trousseau_tables.sql
-- Description: Phase 11 Collaborative Bridal Trousseau Studio & Multi-Party Wedding Wardrobe Curator
-- ==============================================================================

-- 1. Main Trousseau Board
CREATE TABLE IF NOT EXISTS trousseau_boards (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(150) NOT NULL,
    wedding_date DATE NULL,
    notes TEXT NULL,
    share_token VARCHAR(64) NOT NULL UNIQUE,
    is_public_voting BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_trousseau_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_trousseau_user (user_id),
    INDEX idx_trousseau_token (share_token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Ceremony Groups within a Board
CREATE TABLE IF NOT EXISTS trousseau_ceremonies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    board_id BIGINT NOT NULL,
    ceremony_type VARCHAR(50) NOT NULL,
    title VARCHAR(100) NOT NULL,
    color_theme VARCHAR(100) NULL,
    target_budget DECIMAL(10,2) NULL,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_trousseau_ceremony_board FOREIGN KEY (board_id) REFERENCES trousseau_boards (id) ON DELETE CASCADE,
    INDEX idx_ceremony_board (board_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Shortlisted Products in Ceremonies
CREATE TABLE IF NOT EXISTS trousseau_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    board_id BIGINT NOT NULL,
    ceremony_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    added_by_user_id BIGINT NULL,
    is_ai_recommended BOOLEAN NOT NULL DEFAULT FALSE,
    notes TEXT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'SHORTLISTED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_trousseau_item_board FOREIGN KEY (board_id) REFERENCES trousseau_boards (id) ON DELETE CASCADE,
    CONSTRAINT fk_trousseau_item_ceremony FOREIGN KEY (ceremony_id) REFERENCES trousseau_ceremonies (id) ON DELETE CASCADE,
    CONSTRAINT fk_trousseau_item_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE RESTRICT,
    CONSTRAINT fk_trousseau_item_added_by FOREIGN KEY (added_by_user_id) REFERENCES users (id) ON DELETE SET NULL,
    INDEX idx_item_ceremony (ceremony_id),
    INDEX idx_item_product (product_id),
    INDEX idx_item_board (board_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Collaborator Invitations & Access
CREATE TABLE IF NOT EXISTS trousseau_collaborators (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    board_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NULL,
    email VARCHAR(150) NULL,
    role VARCHAR(30) NOT NULL DEFAULT 'VOTER',
    invite_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_trousseau_collab_board FOREIGN KEY (board_id) REFERENCES trousseau_boards (id) ON DELETE CASCADE,
    INDEX idx_collab_board (board_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Multi-Party Votes & Feedback
CREATE TABLE IF NOT EXISTS trousseau_votes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_id BIGINT NOT NULL,
    voter_name VARCHAR(100) NOT NULL,
    voter_phone VARCHAR(20) NULL,
    user_id BIGINT NULL,
    reaction VARCHAR(30) NOT NULL,
    note TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_trousseau_vote_item FOREIGN KEY (item_id) REFERENCES trousseau_items (id) ON DELETE CASCADE,
    CONSTRAINT fk_trousseau_vote_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL,
    INDEX idx_vote_item (item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
