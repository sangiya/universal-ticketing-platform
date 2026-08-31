package com.ticketmesh.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * A connected ticket provider. In the marketplace (Uber/PickMe-style) model a
 * provider is bound to an approved agent shop and exposes the capabilities it
 * supports. Providers may integrate through a native adapter or an outbound API
 * endpoint (stubbed with WireMock in tests / sandbox).
 */
@Entity
@Table(name = "providers")
public class Provider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 40)
    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @NotBlank
    @Size(max = 160)
    @Column(nullable = false, length = 160)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id", nullable = false)
    private AgentShop shop;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @NotBlank
    @Size(min = 2, max = 2)
    @Column(nullable = false, length = 2)
    private String countryIso;

    @NotBlank
    @Size(min = 3, max = 3)
    @Column(nullable = false, length = 3)
    private String currencyIso;

    @NotBlank
    @Size(max = 64)
    @Column(nullable = false, length = 64)
    private String timezone;

    @Size(max = 500)
    @Column(length = 500)
    private String apiEndpoint;

    @NotBlank
    @Size(max = 20)
    @Column(nullable = false, length = 20)
    private String authMode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProviderVertical vertical;

    @Size(max = 255)
    @Column(length = 255)
    private String capabilities;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Size(max = 500)
    @Column(length = 500)
    private String logoUrl;

    @Size(max = 16)
    @Column(length = 16)
    private String themeColor;

    @Size(max = 16)
    @Column(length = 16)
    private String secondaryColor;

    @Size(max = 255)
    @Column(length = 255)
    private String tagline;

    @Size(max = 500)
    @Column(length = 500)
    private String bannerUrl;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public Provider() {
    }

    public Provider(String code, String name, AgentShop shop, Tenant tenant, String countryIso,
                    String currencyIso, String timezone, String apiEndpoint, String authMode,
                    ProviderVertical vertical, String capabilities) {
        this.code = code;
        this.name = name;
        this.shop = shop;
        this.tenant = tenant;
        this.countryIso = countryIso;
        this.currencyIso = currencyIso;
        this.timezone = timezone;
        this.apiEndpoint = apiEndpoint;
        this.authMode = authMode;
        this.vertical = vertical;
        this.capabilities = capabilities;
        this.status = Status.PENDING;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public AgentShop getShop() {
        return shop;
    }

    public Tenant getTenant() {
        return tenant;
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

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getApiEndpoint() {
        return apiEndpoint;
    }

    public void setApiEndpoint(String apiEndpoint) {
        this.apiEndpoint = apiEndpoint;
    }

    public String getAuthMode() {
        return authMode;
    }

    public void setAuthMode(String authMode) {
        this.authMode = authMode;
    }

    public ProviderVertical getVertical() {
        return vertical;
    }

    public String getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(String capabilities) {
        this.capabilities = capabilities;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getThemeColor() {
        return themeColor;
    }

    public void setThemeColor(String themeColor) {
        this.themeColor = themeColor;
    }

    public String getSecondaryColor() {
        return secondaryColor;
    }

    public void setSecondaryColor(String secondaryColor) {
        this.secondaryColor = secondaryColor;
    }

    public String getTagline() {
        return tagline;
    }

    public void setTagline(String tagline) {
        this.tagline = tagline;
    }

    public String getBannerUrl() {
        return bannerUrl;
    }

    public void setBannerUrl(String bannerUrl) {
        this.bannerUrl = bannerUrl;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public enum ProviderVertical {
        BUS,
        TRAIN,
        MOVIE,
        EVENT,
        SPORTS,
        FLIGHT,
        FERRY,
        ATTRACTION,
        OTHER
    }

    public enum Status {
        PENDING,
        ACTIVE,
        SUSPENDED
    }
}
