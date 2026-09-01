-- V12: Hold expiry for marketplace orders (reservation hold timer)
ALTER TABLE product_orders ADD COLUMN hold_expires_at TIMESTAMP NULL;
CREATE INDEX idx_order_hold_expiry ON product_orders (status, hold_expires_at);
-- existing PENDING orders expire in 15 minutes from creation if not paid
UPDATE product_orders SET hold_expires_at = DATEADD('MINUTE', 15, created_at) WHERE status = 'PENDING' AND hold_expires_at IS NULL;
