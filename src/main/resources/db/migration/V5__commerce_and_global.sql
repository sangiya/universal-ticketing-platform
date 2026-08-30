-- V5: Commerce & globalization + higher-level domains
-- Extends the universal ticketing platform for a global, white-label SaaS product.

-- 1. Richer multi-language / multi-currency tenant config (configurable, no code)
ALTER TABLE tenants ADD COLUMN locale VARCHAR(10) NULL;
ALTER TABLE tenants ADD COLUMN country_name VARCHAR(120) NULL;
ALTER TABLE tenants ADD COLUMN supported_currencies VARCHAR(500) NULL;
ALTER TABLE tenants ADD COLUMN supported_languages VARCHAR(500) NULL;

-- 2. Richer white-label theming (WordPress / Uber / PickMe style full UI config)
ALTER TABLE tenant_branding ADD COLUMN hero_image_url VARCHAR(500) NULL;
ALTER TABLE tenant_branding ADD COLUMN footer_text VARCHAR(500) NULL;
ALTER TABLE tenant_branding ADD COLUMN currency_symbol_position VARCHAR(10) NULL;
ALTER TABLE tenant_branding ADD COLUMN button_radius INT NULL;
ALTER TABLE tenant_branding ADD COLUMN nav_theme VARCHAR(20) NULL;
ALTER TABLE tenant_branding ADD COLUMN custom_css CLOB NULL;
ALTER TABLE tenant_branding ADD COLUMN favicon_url VARCHAR(500) NULL;
ALTER TABLE tenant_branding ADD COLUMN announcement VARCHAR(500) NULL;

-- 3. Pricing & fare engine: explicit price components (fees / taxes)
ALTER TABLE provider_products ADD COLUMN base_price DECIMAL(12, 2) NULL;
ALTER TABLE provider_products ADD COLUMN tax_rate DECIMAL(6, 4) NULL;
ALTER TABLE provider_products ADD COLUMN service_fee DECIMAL(12, 2) NULL;
ALTER TABLE provider_products ADD COLUMN seat_layout CLOB NULL;
ALTER TABLE provider_products ADD COLUMN venue VARCHAR(160) NULL;
ALTER TABLE provider_products ADD COLUMN starts_at TIMESTAMP NULL;
ALTER TABLE provider_products ADD COLUMN ends_at TIMESTAMP NULL;

-- 4. i18n message dictionary (multi-language, per tenant + fallback global)
CREATE TABLE i18n_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT,
    locale VARCHAR(10) NOT NULL,
    message_key VARCHAR(120) NOT NULL,
    message_value VARCHAR(1000) NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_i18n UNIQUE (tenant_id, locale, message_key)
);

-- 5. Currency exchange rates (multi-currency conversion)
CREATE TABLE exchange_rates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT,
    base_currency VARCHAR(3) NOT NULL,
    target_currency VARCHAR(3) NOT NULL,
    rate DECIMAL(18, 8) NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_rate UNIQUE (tenant_id, base_currency, target_currency)
);

-- 6. Promotion engine (coupons / promo rules)
CREATE TABLE promotions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    code VARCHAR(40) NOT NULL,
    name VARCHAR(120) NOT NULL,
    discount_type VARCHAR(12) NOT NULL,
    discount_value DECIMAL(12, 2) NOT NULL,
    min_purchase DECIMAL(12, 2) NULL,
    starts_at TIMESTAMP NULL,
    ends_at TIMESTAMP NULL,
    max_uses INT NULL,
    used_count INT NOT NULL DEFAULT 0,
    domains VARCHAR(255),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_promo UNIQUE (tenant_id, code)
);

-- 7. Customer & loyalty
CREATE TABLE loyalty_accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    points BIGINT NOT NULL DEFAULT 0,
    lifetime_points BIGINT NOT NULL DEFAULT 0,
    tier VARCHAR(20) NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_loyalty_user UNIQUE (tenant_id, user_id)
);

-- 8. Reviews (customer -> product)
CREATE TABLE reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    product_id BIGINT,
    user_id BIGINT NOT NULL,
    rating INT NOT NULL,
    title VARCHAR(160),
    comment VARCHAR(1000),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT chk_rating CHECK (rating BETWEEN 1 AND 5)
);

-- 9. High-security audit trail
CREATE TABLE audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT,
    actor_username VARCHAR(50),
    action VARCHAR(80) NOT NULL,
    resource_type VARCHAR(60),
    resource_ref VARCHAR(120),
    detail VARCHAR(1000),
    ip_address VARCHAR(45),
    created_at TIMESTAMP NOT NULL
);

-- 10. Notification service
CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT,
    recipient_id BIGINT NOT NULL,
    channel VARCHAR(20) NOT NULL,
    subject VARCHAR(200),
    body VARCHAR(2000) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

-- 11. Outbox / event-driven architecture
CREATE TABLE outbox_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id VARCHAR(64) NOT NULL,
    aggregate_type VARCHAR(60) NOT NULL,
    aggregate_id VARCHAR(60) NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    payload CLOB NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    delivered_at TIMESTAMP,
    CONSTRAINT uq_event UNIQUE (event_id)
);

-- 12. Trip / multi-ticket journey (smart trip planner)
CREATE TABLE trips (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    title VARCHAR(200),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE trip_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    trip_id BIGINT NOT NULL,
    booking_id BIGINT,
    position INT NOT NULL,
    note VARCHAR(500),
    CONSTRAINT fk_trip_item_trip FOREIGN KEY (trip_id) REFERENCES trips (id),
    CONSTRAINT fk_trip_item_booking FOREIGN KEY (booking_id) REFERENCES bookings (id)
);
