-- V8: Social features and PII protection.
-- Adds family groups, per-user settings, referrals / invite-friends, and
-- encrypted-at-rest PII copies, plus promotion kinds (PROMO / VOUCHER / OFFER).

-- 1. Users: encrypted at-rest copies of email / phone written in parallel so
--      plaintext lookups (login, uniqueness) keep working while strong
--      encryption protects the sensitive values at rest.
ALTER TABLE users ADD COLUMN email_encrypted VARCHAR(512) NULL;
ALTER TABLE users ADD COLUMN phone_encrypted VARCHAR(512) NULL;

-- 2. Family groups: a tenant-scoped group of users sharing benefits.
CREATE TABLE family_groups (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    name VARCHAR(120) NOT NULL,
    owner_user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_family_owner FOREIGN KEY (owner_user_id) REFERENCES users (id)
);

-- 3. Family members: one row per (group, user) with an owner/member role.
CREATE TABLE family_members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    joined_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_family_member UNIQUE (family_id, user_id),
    CONSTRAINT fk_member_family FOREIGN KEY (family_id) REFERENCES family_groups (id),
    CONSTRAINT fk_member_user FOREIGN KEY (user_id) REFERENCES users (id)
);

-- 4. Per-user settings / preferences, created lazily on first access.
CREATE TABLE user_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    theme VARCHAR(20) NOT NULL DEFAULT 'LIGHT',
    language VARCHAR(8) NOT NULL DEFAULT 'en',
    currency VARCHAR(3) NOT NULL DEFAULT 'LKR',
    notify_email BOOLEAN NOT NULL DEFAULT TRUE,
    notify_sms BOOLEAN NOT NULL DEFAULT FALSE,
    notify_push BOOLEAN NOT NULL DEFAULT TRUE,
    notify_whatsapp BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_settings_user UNIQUE (user_id),
    CONSTRAINT fk_settings_user FOREIGN KEY (user_id) REFERENCES users (id)
);

-- 5. Referrals / invite-friends. The invitee email is stored encrypted at
--      rest by the service layer; only the referrer and reward state are kept.
CREATE TABLE referrals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    referrer_user_id BIGINT NOT NULL,
    invitee_user_id BIGINT,
    invitee_email VARCHAR(512),
    code VARCHAR(24) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reward_points BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    joined_at TIMESTAMP,
    CONSTRAINT uq_referral_code UNIQUE (code),
    CONSTRAINT fk_referral_referrer FOREIGN KEY (referrer_user_id) REFERENCES users (id)
);

-- 6. Promotions: classify rules as general PROMO, VOUCHER (flat) or OFFER
--      (multi-buy / fixed-price tag in the domains field).
ALTER TABLE promotions ADD COLUMN kind VARCHAR(16) NOT NULL DEFAULT 'PROMO';
