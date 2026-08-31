-- V18: 2FA TOTP
CREATE TABLE user_mfa (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    secret_enc VARCHAR(500),
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    backup_codes_enc VARCHAR(2000),
    verified_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_mfa_user FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX idx_mfa_user (user_id)
);
