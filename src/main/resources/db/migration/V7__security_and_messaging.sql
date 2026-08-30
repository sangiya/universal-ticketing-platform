-- V7: Security (2FA/OTP/app-key), identity verification, moderation mode,
--      messaging/omnichannel channels, and extended payment methods.
-- Turns the platform into a fully open, Uber/PickMe-style marketplace with
-- strong onboarding security and WhatsApp/Facebook/SMS channel integration.

-- 1. Users: contact + multi-factor auth fields (OTP / TOTP / app-key)
ALTER TABLE users ADD COLUMN phone VARCHAR(40) NULL;
ALTER TABLE users ADD COLUMN two_factor_method VARCHAR(20) NULL;
ALTER TABLE users ADD COLUMN totp_secret VARCHAR(128) NULL;
ALTER TABLE users ADD COLUMN totp_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN app_key_hash VARCHAR(255) NULL;
ALTER TABLE users ADD COLUMN app_key_issued_at TIMESTAMP NULL;

-- 2. Tenants: moderation mode (INSTANT = open Uber/PickMe activation,
--      REVIEW = admin-approve before an agent can sell)
ALTER TABLE tenants ADD COLUMN moderation_mode VARCHAR(12) NOT NULL DEFAULT 'INSTANT';

-- 3. Identity verification for agents (NIC / passport / driving licence + photo).
--      Stronger onboarding per the security requirement.
CREATE TABLE identity_verifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    doc_type VARCHAR(20) NOT NULL,
    doc_number VARCHAR(64) NOT NULL,
    doc_photo_url VARCHAR(500),
    status VARCHAR(20) NOT NULL,
    verified_by BIGINT,
    verified_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_identity_user UNIQUE (user_id),
    CONSTRAINT fk_identity_user FOREIGN KEY (user_id) REFERENCES users (id)
);

-- 4. One-time passcodes (email / SMS OTP, 2FA challenge codes)
CREATE TABLE otp_codes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    purpose VARCHAR(20) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    code_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    consumed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_otp_user FOREIGN KEY (user_id) REFERENCES users (id)
);

-- 5. Outbound channel integrations (WhatsApp / Facebook / Telegram / SMS)
--      URL + secret are kept via environment-seeded secrets; this table stores
--      the per-tenant integration config reference.
CREATE TABLE channel_integrations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    channel VARCHAR(20) NOT NULL,
    name VARCHAR(120) NOT NULL,
    api_key_ref VARCHAR(120),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_channel_integration UNIQUE (tenant_id, channel),
    CONSTRAINT fk_channel_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id)
);

-- 6. Messaging conversation (inbound webhook + outbound channel messages)
CREATE TABLE messaging_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    channel VARCHAR(20) NOT NULL,
    direction VARCHAR(10) NOT NULL,
    external_ref VARCHAR(120),
    sender_ref VARCHAR(120),
    recipient_ref VARCHAR(120),
    body VARCHAR(2000) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_msg_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id)
);

CREATE INDEX idx_msg_tenant_channel ON messaging_messages (tenant_id, channel);
CREATE INDEX idx_msg_external_ref ON messaging_messages (external_ref);
