-- V23__cancellation_and_refunds.sql
-- Spec section 26: cancellation policy + refund engine.
--
-- refund_policy_windows holds the tiered refund rules a provider configures per
-- product. A window applies once the event is at least min_hours_before_event
-- hours away; the first satisfied window (largest bound first) wins.
--
-- provider_products gains a JSON cancellation summary for display, and
-- product_orders gains the refund outcome fields plus loyalty_points_granted so
-- a refund can claw back exactly the points that were awarded.
--
-- MySQL: ADD COLUMN has no IF NOT EXISTS; migrations run once on a fresh schema.

CREATE TABLE refund_policy_windows (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    min_hours_before_event INT NOT NULL,
    refund_percent DECIMAL(5,2) NOT NULL,
    fee_amount DECIMAL(12,2) NULL,
    fee_percent DECIMAL(5,2) NULL,
    label VARCHAR(120) NULL,
    CONSTRAINT fk_refund_window_product FOREIGN KEY (product_id) REFERENCES provider_products (id),
    INDEX idx_refund_window_product (product_id, min_hours_before_event)
);

ALTER TABLE provider_products ADD COLUMN cancellation_policy LONGTEXT NULL;

ALTER TABLE product_orders ADD COLUMN cancelled_at TIMESTAMP NULL;
ALTER TABLE product_orders ADD COLUMN cancellation_reason VARCHAR(255) NULL;
ALTER TABLE product_orders ADD COLUMN refunded_at TIMESTAMP NULL;
ALTER TABLE product_orders ADD COLUMN refunded_amount DECIMAL(12,2) NULL;
ALTER TABLE product_orders ADD COLUMN cancellation_fee DECIMAL(12,2) NULL;
ALTER TABLE product_orders ADD COLUMN policy_snapshot LONGTEXT NULL;
ALTER TABLE product_orders ADD COLUMN loyalty_points_granted BIGINT NOT NULL DEFAULT 0;
ALTER TABLE product_orders ADD COLUMN refund_reference VARCHAR(64) NULL;
