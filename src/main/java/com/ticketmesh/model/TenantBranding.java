package com.ticketmesh.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

/**
 * WordPress-like theme/branding for a tenant. A shop owner can change colors,
 * images, logo, fonts and hero copy purely through configuration (published by
 * the platform admin or, in the agent self-service model, by the shop owner)
 * without touching application binaries.
 */
@Entity
@Table(name = "tenant_branding")
public class TenantBranding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false, unique = true)
    private Tenant tenant;

    @NotBlank
    @Column(nullable = false, length = 160)
    private String brandName;

    @Column(length = 255)
    private String tagline;

    @NotBlank
    @Column(nullable = false, length = 16)
    private String primaryColor;

    @Column(length = 16)
    private String secondaryColor;

    @Column(length = 16)
    private String accentColor;

    @Column(length = 500)
    private String logoUrl;

    @Column(length = 500)
    private String bannerUrl;

    @Column(length = 500)
    private String appIconUrl;

    @Column(length = 120)
    private String fontFamily;

    @Column(nullable = false)
    private int borderRadius = 8;

    @Column(nullable = false)
    private boolean darkMode = false;

    @Column(length = 255)
    private String homeHeroTitle;

    @Column(length = 500)
    private String homeHeroSubtitle;

    @Column(nullable = false)
    private Instant updatedAt;

    public TenantBranding() {
    }

    public TenantBranding(Tenant tenant, String brandName, String primaryColor) {
        this.tenant = tenant;
        this.brandName = brandName;
        this.primaryColor = primaryColor;
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(String brandName) {
        this.brandName = brandName;
    }

    public String getTagline() {
        return tagline;
    }

    public void setTagline(String tagline) {
        this.tagline = tagline;
    }

    public String getPrimaryColor() {
        return primaryColor;
    }

    public void setPrimaryColor(String primaryColor) {
        this.primaryColor = primaryColor;
    }

    public String getSecondaryColor() {
        return secondaryColor;
    }

    public void setSecondaryColor(String secondaryColor) {
        this.secondaryColor = secondaryColor;
    }

    public String getAccentColor() {
        return accentColor;
    }

    public void setAccentColor(String accentColor) {
        this.accentColor = accentColor;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getBannerUrl() {
        return bannerUrl;
    }

    public void setBannerUrl(String bannerUrl) {
        this.bannerUrl = bannerUrl;
    }

    public String getAppIconUrl() {
        return appIconUrl;
    }

    public void setAppIconUrl(String appIconUrl) {
        this.appIconUrl = appIconUrl;
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public void setFontFamily(String fontFamily) {
        this.fontFamily = fontFamily;
    }

    public int getBorderRadius() {
        return borderRadius;
    }

    public void setBorderRadius(int borderRadius) {
        this.borderRadius = borderRadius;
    }

    public boolean isDarkMode() {
        return darkMode;
    }

    public void setDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
    }

    public String getHomeHeroTitle() {
        return homeHeroTitle;
    }

    public void setHomeHeroTitle(String homeHeroTitle) {
        this.homeHeroTitle = homeHeroTitle;
    }

    public String getHomeHeroSubtitle() {
        return homeHeroSubtitle;
    }

    public void setHomeHeroSubtitle(String homeHeroSubtitle) {
        this.homeHeroSubtitle = homeHeroSubtitle;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }
}
