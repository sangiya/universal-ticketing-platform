-- V10: Per-provider branding — every provider/shop can customize its own look & feel.
ALTER TABLE providers ADD COLUMN logo_url VARCHAR(500) NULL;
ALTER TABLE providers ADD COLUMN theme_color VARCHAR(16) NULL;
ALTER TABLE providers ADD COLUMN secondary_color VARCHAR(16) NULL;
ALTER TABLE providers ADD COLUMN tagline VARCHAR(255) NULL;
ALTER TABLE providers ADD COLUMN banner_url VARCHAR(500) NULL;
