-- V17: Wallet credits (spec 29)
CREATE TABLE wallets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL UNIQUE,
    balance DECIMAL(12,2) NOT NULL DEFAULT 0,
    currency_iso VARCHAR(3) NOT NULL DEFAULT 'LKR',
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_wallet_user FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX idx_wallet_user (user_id)
);
CREATE TABLE wallet_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    wallet_id BIGINT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    type VARCHAR(20) NOT NULL,
    reason VARCHAR(255),
    order_ref VARCHAR(40),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_wt_wallet FOREIGN KEY (wallet_id) REFERENCES wallets (id),
    INDEX idx_wt_wallet (wallet_id, created_at)
);
