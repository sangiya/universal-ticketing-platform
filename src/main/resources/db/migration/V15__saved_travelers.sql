-- V15: Saved travelers with PII encryption and consent
CREATE TABLE saved_travelers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    owner_user_id BIGINT NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    relationship VARCHAR(20) NOT NULL DEFAULT 'OTHER',
    date_of_birth DATE,
    gender VARCHAR(20) NOT NULL DEFAULT 'UNSPECIFIED',
    nationality VARCHAR(2),
    document_type VARCHAR(30),
    document_number_enc VARCHAR(500),
    phone_enc VARCHAR(500),
    email_enc VARCHAR(500),
    consent_given BOOLEAN NOT NULL DEFAULT FALSE,
    consent_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_traveler_owner FOREIGN KEY (owner_user_id) REFERENCES users (id),
    UNIQUE KEY uk_traveler_owner (owner_user_id, full_name, date_of_birth),
    INDEX idx_traveler_owner (owner_user_id),
    INDEX idx_traveler_tenant (tenant_id)
);
