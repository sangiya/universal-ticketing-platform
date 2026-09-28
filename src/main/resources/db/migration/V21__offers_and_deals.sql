-- V11__offers_and_deals.sql
-- Adds offer/deal fields to provider_products table so individual tickets can be
-- marked as special-time-limited deals (distinct from discount coupon codes).

ALTER TABLE provider_products ADD COLUMN is_offer BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE provider_products ADD COLUMN original_price DECIMAL(12,2) DEFAULT NULL;
ALTER TABLE provider_products ADD COLUMN discount_percent INT DEFAULT NULL;
ALTER TABLE provider_products ADD COLUMN deal_type VARCHAR(20) DEFAULT NULL;
ALTER TABLE provider_products ADD COLUMN deal_tag VARCHAR(40) DEFAULT NULL;
ALTER TABLE provider_products ADD COLUMN deal_valid_until TIMESTAMP NULL DEFAULT NULL;
ALTER TABLE provider_products ADD COLUMN deal_seats INT DEFAULT NULL;
ALTER TABLE provider_products ADD COLUMN thumbnail_url VARCHAR(500) DEFAULT NULL;
