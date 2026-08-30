-- V6: Universal marketplace product orders
-- Customers can buy ANY provider product (bus/train/movie/event/etc.) through the
-- universal booking engine, alongside the schedule-based train bookings.

CREATE TABLE product_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_ref VARCHAR(40) NOT NULL UNIQUE,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    provider_name VARCHAR(160),
    product_title VARCHAR(200) NOT NULL,
    product_type VARCHAR(30) NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(12, 2) NOT NULL,
    currency_iso VARCHAR(3) NOT NULL,
    base_amount DECIMAL(12, 2) NOT NULL,
    tax_amount DECIMAL(12, 2) NOT NULL,
    service_fee DECIMAL(12, 2) NOT NULL,
    discount_amount DECIMAL(12, 2) NOT NULL DEFAULT 0,
    total_amount DECIMAL(12, 2) NOT NULL,
    promo_code VARCHAR(40),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    paid_at TIMESTAMP,
    CONSTRAINT fk_order_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_order_product FOREIGN KEY (product_id) REFERENCES provider_products (id)
);

CREATE INDEX idx_order_user ON product_orders (user_id);
CREATE INDEX idx_order_tenant ON product_orders (tenant_id);
