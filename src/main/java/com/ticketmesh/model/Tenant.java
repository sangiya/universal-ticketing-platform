package com.ticketmesh.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * A white-label SaaS tenant. Every tenant is fully configurable for its own
 * country (ISO 3166-1 alpha-2), currency (ISO 4217), default language
 * (BCP-47) and timezone so the product can be sold globally.
 */
@Entity
@Table(name = "tenants")
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 60)
    @Column(nullable = false, unique = true, length = 60)
    private String slug;

    @NotBlank
    @Size(max = 160)
    @Column(nullable = false, length = 160)
    private String name;

    @NotBlank
    @Size(min = 2, max = 2)
    @Column(nullable = false, length = 2)
    private String countryIso;

    @NotBlank
    @Size(min = 3, max = 3)
    @Column(nullable = false, length = 3)
    private String currencyIso;

    @NotBlank
    @Size(max = 8)
    @Column(nullable = false, length = 8)
    private String defaultLanguage;

    @NotBlank
    @Size(max = 64)
    @Column(nullable = false, length = 64)
    private String timezone;

    @Size(max = 160)
    @Column(length = 160)
    private String domain;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private int configVersion = 1;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public Tenant() {
    }

    public Tenant(String slug, String name, String countryIso, String currencyIso,
                  String defaultLanguage, String timezone, String domain) {
        this.slug = slug;
        this.name = name;
        this.countryIso = countryIso;
        this.currencyIso = currencyIso;
        this.defaultLanguage = defaultLanguage;
        this.timezone = timezone;
        this.domain = domain;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCountryIso() {
        return countryIso;
    }

    public void setCountryIso(String countryIso) {
        this.countryIso = countryIso;
    }

    public String getCurrencyIso() {
        return currencyIso;
    }

    public void setCurrencyIso(String currencyIso) {
        this.currencyIso = currencyIso;
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public void setDefaultLanguage(String defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        this.updatedAt = Instant.now();
    }

    public int getConfigVersion() {
        return configVersion;
    }

    public void bumpConfigVersion() {
        this.configVersion = this.configVersion + 1;
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
