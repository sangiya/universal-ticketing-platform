-- V19: KYC / KYB submissions (spec 3)
CREATE TABLE kyc_submissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    shop_id BIGINT,
    document_type VARCHAR(30),
    document_number_enc VARCHAR(500),
    document_url VARCHAR(500),
    selfie_url VARCHAR(500),
    business_reg VARCHAR(100),
    status VARCHAR(25) NOT NULL,
    internal_reason VARCHAR(255),
    customer_reason VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    reviewed_at TIMESTAMP NULL,
    reviewed_by BIGINT,
    CONSTRAINT fk_kyc_user FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX idx_kyc_user (user_id),
    INDEX idx_kyc_status (status)
);
