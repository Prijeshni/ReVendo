-- =========================================================
--  ReVendo Database Schema
--  IF NOT EXISTS makes every statement safe to re-run.
--  Tables are ordered: parents before children (FK safety).
-- =========================================================

-- 1. users
CREATE TABLE IF NOT EXISTS users (
    id                    BIGINT       NOT NULL AUTO_INCREMENT,
    first_name            VARCHAR(50)  NOT NULL,
    last_name             VARCHAR(50)  NOT NULL,
    email                 VARCHAR(254) NOT NULL,
    phone_number          VARCHAR(15)  NOT NULL,
    password_hash         VARCHAR(255) NOT NULL,
    role                  ENUM('ROLE_USER','ROLE_ADMIN') NOT NULL DEFAULT 'ROLE_USER',
    status                ENUM('Active','Suspended')     NOT NULL DEFAULT 'Active',
    failed_login_attempts INT          NOT NULL DEFAULT 0,
    locked_until          DATETIME     NULL,
    created_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email),
    INDEX idx_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. listings
CREATE TABLE IF NOT EXISTS listings (
    id                 BIGINT        NOT NULL AUTO_INCREMENT,
    seller_id          BIGINT        NOT NULL,
    title              VARCHAR(150)  NOT NULL,
    description        TEXT          NOT NULL,
    category           ENUM('Electronics','Furniture','Books','Home Appliances','Household Goods') NOT NULL,
    condition_type     ENUM('New','Like New','Good','Fair','Poor') NOT NULL,
    listing_type       ENUM('FOR_SALE','FOR_RENT') NOT NULL,
    price              DECIMAL(12,2) NOT NULL,
    rental_period_unit ENUM('PER_DAY','PER_WEEK','PER_MONTH') NULL,
    status             ENUM('Active','Sold','Rented','Deleted','Removed','Suspended') NOT NULL DEFAULT 'Active',
    created_at         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_listings_seller_id  (seller_id),
    INDEX idx_listings_created_at (created_at),
    INDEX idx_listings_status     (status),
    CONSTRAINT fk_listings_seller FOREIGN KEY (seller_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. listing_images
CREATE TABLE IF NOT EXISTS listing_images (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    listing_id    BIGINT       NOT NULL,
    image_url     VARCHAR(500) NOT NULL,
    display_order INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    INDEX idx_listing_images_listing_id (listing_id),
    CONSTRAINT fk_listing_images_listing FOREIGN KEY (listing_id) REFERENCES listings (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. transactions
CREATE TABLE IF NOT EXISTS transactions (
    id                 BIGINT        NOT NULL AUTO_INCREMENT,
    listing_id         BIGINT        NOT NULL,
    buyer_id           BIGINT        NOT NULL,
    transaction_type   ENUM('PURCHASE','RENTAL') NOT NULL,
    price_paid         DECIMAL(12,2) NOT NULL,
    transaction_date   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    rental_start_date  DATE          NULL,
    rental_end_date    DATE          NULL,
    rental_duration    INT           NULL,
    rental_period_unit ENUM('PER_DAY','PER_WEEK','PER_MONTH') NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_transactions_listing (listing_id),
    INDEX idx_transactions_buyer_id (buyer_id),
    CONSTRAINT fk_transactions_listing FOREIGN KEY (listing_id) REFERENCES listings (id),
    CONSTRAINT fk_transactions_buyer   FOREIGN KEY (buyer_id)   REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. offers
CREATE TABLE IF NOT EXISTS offers (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    listing_id     BIGINT        NOT NULL,
    buyer_id       BIGINT        NOT NULL,
    offer_amount   DECIMAL(12,2) NOT NULL,
    counter_amount DECIMAL(12,2) NULL,
    status         ENUM('Pending','Accepted','Rejected','Countered') NOT NULL DEFAULT 'Pending',
    round_count    INT           NOT NULL DEFAULT 0,
    created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_offers_listing_id (listing_id),
    INDEX idx_offers_buyer_id   (buyer_id),
    CONSTRAINT fk_offers_listing FOREIGN KEY (listing_id) REFERENCES listings (id),
    CONSTRAINT fk_offers_buyer   FOREIGN KEY (buyer_id)   REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. conversations
CREATE TABLE IF NOT EXISTS conversations (
    id              BIGINT   NOT NULL AUTO_INCREMENT,
    listing_id      BIGINT   NOT NULL,
    buyer_id        BIGINT   NOT NULL,
    seller_id       BIGINT   NOT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_message_at DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_conversations_triple (listing_id, buyer_id, seller_id),
    INDEX idx_conversations_buyer_id  (buyer_id),
    INDEX idx_conversations_seller_id (seller_id),
    CONSTRAINT fk_conversations_listing FOREIGN KEY (listing_id) REFERENCES listings (id),
    CONSTRAINT fk_conversations_buyer   FOREIGN KEY (buyer_id)   REFERENCES users (id),
    CONSTRAINT fk_conversations_seller  FOREIGN KEY (seller_id)  REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. messages
CREATE TABLE IF NOT EXISTS messages (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    conversation_id BIGINT        NOT NULL,
    sender_id       BIGINT        NOT NULL,
    msg_text        VARCHAR(2000) NOT NULL,
    is_read         BOOLEAN       NOT NULL DEFAULT FALSE,
    sent_at         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_messages_conversation_id (conversation_id),
    INDEX idx_messages_sent_at         (sent_at),
    CONSTRAINT fk_messages_conversation FOREIGN KEY (conversation_id) REFERENCES conversations (id) ON DELETE CASCADE,
    CONSTRAINT fk_messages_sender       FOREIGN KEY (sender_id)       REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. wishlist_items
CREATE TABLE IF NOT EXISTS wishlist_items (
    id         BIGINT   NOT NULL AUTO_INCREMENT,
    user_id    BIGINT   NOT NULL,
    listing_id BIGINT   NOT NULL,
    added_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_wishlist_user_listing (user_id, listing_id),
    INDEX idx_wishlist_user_id (user_id),
    CONSTRAINT fk_wishlist_user    FOREIGN KEY (user_id)    REFERENCES users (id),
    CONSTRAINT fk_wishlist_listing FOREIGN KEY (listing_id) REFERENCES listings (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. notifications
CREATE TABLE IF NOT EXISTS notifications (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT       NOT NULL,
    message    VARCHAR(500) NOT NULL,
    is_read    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_notifications_user_id (user_id),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. purchase_requests
CREATE TABLE IF NOT EXISTS purchase_requests (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    listing_id      BIGINT        NOT NULL,
    buyer_id        BIGINT        NOT NULL,
    seller_id       BIGINT        NOT NULL,
    price_at_request DECIMAL(12,2) NOT NULL,
    request_type    ENUM('PURCHASE','RENTAL') NOT NULL DEFAULT 'PURCHASE',
    rental_duration INT           NULL,
    status          ENUM('PENDING','ACCEPTED','REJECTED') NOT NULL DEFAULT 'PENDING',
    request_date    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    response_date   DATETIME      NULL,
    PRIMARY KEY (id),
    INDEX idx_pr_listing_id (listing_id),
    INDEX idx_pr_buyer_id   (buyer_id),
    INDEX idx_pr_seller_id  (seller_id),
    CONSTRAINT fk_pr_listing FOREIGN KEY (listing_id) REFERENCES listings (id),
    CONSTRAINT fk_pr_buyer   FOREIGN KEY (buyer_id)   REFERENCES users (id),
    CONSTRAINT fk_pr_seller  FOREIGN KEY (seller_id)  REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
