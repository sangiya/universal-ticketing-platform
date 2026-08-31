-- V13: Loyalty ledger (accrual/redemption/expiration per spec 29)
CREATE TABLE loyalty_ledger (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    points_delta BIGINT NOT NULL,
    balance_after BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    reason VARCHAR(255),
    order_ref VARCHAR(40),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_ledger_user FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX idx_ledger_user (tenant_id, user_id, created_at)
);
