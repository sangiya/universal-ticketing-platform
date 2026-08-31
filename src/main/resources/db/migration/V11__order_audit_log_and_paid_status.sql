-- V11: Order audit trail and lifecycle hardening (PENDING -> PAID -> ISSUED)
CREATE TABLE order_audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    from_status VARCHAR(20) NOT NULL,
    to_status VARCHAR(20) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    changed_by VARCHAR(100) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    CONSTRAINT fk_audit_order FOREIGN KEY (order_id) REFERENCES product_orders (id)
);
CREATE INDEX idx_audit_order ON order_audit_logs (order_id);

-- Migrate legacy CONFIRMED -> PAID (new lifecycle: PENDING -> PAID -> ISSUED)
UPDATE product_orders SET status = 'PAID' WHERE status = 'CONFIRMED';
