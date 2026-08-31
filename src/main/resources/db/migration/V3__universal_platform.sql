ALTER TABLE users ADD COLUMN tenant_id BIGINT NULL;
ALTER TABLE users ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

CREATE TABLE tenants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    slug VARCHAR(60) NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    country_iso VARCHAR(2) NOT NULL,
    currency_iso VARCHAR(3) NOT NULL,
    default_language VARCHAR(8) NOT NULL,
    timezone VARCHAR(64) NOT NULL,
    domain VARCHAR(160),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    config_version INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE tenant_branding (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    brand_name VARCHAR(160) NOT NULL,
    tagline VARCHAR(255),
    primary_color VARCHAR(16) NOT NULL,
    secondary_color VARCHAR(16),
    accent_color VARCHAR(16),
    logo_url VARCHAR(500),
    banner_url VARCHAR(500),
    app_icon_url VARCHAR(500),
    font_family VARCHAR(120),
    border_radius INT NOT NULL DEFAULT 8,
    dark_mode BOOLEAN NOT NULL DEFAULT FALSE,
    home_hero_title VARCHAR(255),
    home_hero_subtitle VARCHAR(500),
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_branding_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT uq_branding_tenant UNIQUE (tenant_id)
);

CREATE TABLE agent_shops (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_user_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    shop_name VARCHAR(160) NOT NULL,
    business_type VARCHAR(80) NOT NULL,
    country_iso VARCHAR(2) NOT NULL,
    currency_iso VARCHAR(3) NOT NULL,
    about VARCHAR(500),
    contact_email VARCHAR(160),
    contact_phone VARCHAR(40),
    status VARCHAR(20) NOT NULL,
    applied_at TIMESTAMP NOT NULL,
    reviewed_at TIMESTAMP,
    reviewed_by BIGINT,
    CONSTRAINT fk_shop_owner FOREIGN KEY (owner_user_id) REFERENCES users (id),
    CONSTRAINT fk_shop_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id)
);

CREATE TABLE providers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    shop_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    country_iso VARCHAR(2) NOT NULL,
    currency_iso VARCHAR(3) NOT NULL,
    timezone VARCHAR(64) NOT NULL,
    api_endpoint VARCHAR(500),
    auth_mode VARCHAR(20) NOT NULL,
    vertical VARCHAR(30) NOT NULL,
    capabilities VARCHAR(255),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_provider_shop FOREIGN KEY (shop_id) REFERENCES agent_shops (id),
    CONSTRAINT fk_provider_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id)
);

CREATE TABLE provider_products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    provider_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    product_type VARCHAR(30) NOT NULL,
    title VARCHAR(200) NOT NULL,
    origin VARCHAR(120),
    destination VARCHAR(120),
    event_date TIMESTAMP,
    price DECIMAL(12, 2) NOT NULL,
    currency_iso VARCHAR(3) NOT NULL,
    available_quantity INT NOT NULL DEFAULT 0,
    description VARCHAR(1000),
    attributes LONGTEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_product_provider FOREIGN KEY (provider_id) REFERENCES providers (id),
    CONSTRAINT fk_product_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id)
);
