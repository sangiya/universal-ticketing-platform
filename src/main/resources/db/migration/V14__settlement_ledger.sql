-- V14: Settlement ledger (immutable per-order financial breakdown)
CREATE TABLE settlement_entries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    order_ref VARCHAR(40) NOT NULL,
    provider_id BIGINT,
    product_type VARCHAR(30),
    gross_amount DECIMAL(12,2) NOT NULL,
    platform_fee DECIMAL(12,2) NOT NULL,
    net_payout DECIMAL(12,2) NOT NULL,
    currency_iso VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_settlement_order FOREIGN KEY (order_ref) REFERENCES product_orders (order_ref),
    INDEX idx_settlement_tenant (tenant_id, created_at),
    INDEX idx_settlement_order (order_ref)
);
