-- V22__movies_metadata.sql
-- Adds movie-specific metadata fields to provider_products so that
-- ADMISSION products (movies, concerts, attractions) can be browsed
-- and filtered like a proper movie ticket page (BookMyShow style).

ALTER TABLE provider_products ADD COLUMN language VARCHAR(40) DEFAULT NULL;
ALTER TABLE provider_products ADD COLUMN genre VARCHAR(120) DEFAULT NULL;
ALTER TABLE provider_products ADD COLUMN format VARCHAR(40) DEFAULT NULL;
ALTER TABLE provider_products ADD COLUMN duration_minutes INT DEFAULT NULL;
ALTER TABLE provider_products ADD COLUMN rating_stars DECIMAL(2,1) DEFAULT NULL;
ALTER TABLE provider_products ADD COLUMN cast_list VARCHAR(500) DEFAULT NULL;
ALTER TABLE provider_products ADD COLUMN director VARCHAR(160) DEFAULT NULL;
ALTER TABLE provider_products ADD COLUMN release_date DATE DEFAULT NULL;
ALTER TABLE provider_products ADD COLUMN poster_url VARCHAR(500) DEFAULT NULL;
ALTER TABLE provider_products ADD COLUMN banner_url VARCHAR(500) DEFAULT NULL;
ALTER TABLE provider_products ADD COLUMN is_premiere TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE provider_products ADD COLUMN is_now_showing TINYINT(1) NOT NULL DEFAULT 1;
ALTER TABLE provider_products ADD COLUMN tagline VARCHAR(200) DEFAULT NULL;

CREATE INDEX idx_pp_language ON provider_products (language);
CREATE INDEX idx_pp_genre ON provider_products (genre);
CREATE INDEX idx_pp_format ON provider_products (format);
