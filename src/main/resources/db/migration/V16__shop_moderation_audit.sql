-- V16: Moderation governance — suspension reason + audit
ALTER TABLE agent_shops ADD COLUMN suspension_reason VARCHAR(255) NULL;

CREATE TABLE shop_moderation_audit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    shop_id BIGINT NOT NULL,
    from_status VARCHAR(20) NOT NULL,
    to_status VARCHAR(20) NOT NULL,
    reason VARCHAR(255),
    actor_user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_audit_shop FOREIGN KEY (shop_id) REFERENCES agent_shops (id),
    INDEX idx_audit_shop (shop_id, created_at)
);
