package com.ticketmesh.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class BrandingRequest {

    @NotBlank
    @Size(max = 160)
    private String brandName;

    @Size(max = 255)
    private String tagline;

    @NotBlank
    @Size(max = 16)
    private String primaryColor;

    @Size(max = 16)
    private String secondaryColor;

    @Size(max = 16)
    private String accentColor;

    @Size(max = 500)
    private String logoUrl;

    @Size(max = 500)
    private String bannerUrl;

    @Size(max = 500)
    private String appIconUrl;

    @Size(max = 120)
    private String fontFamily;

    private Integer borderRadius;

    private Boolean darkMode;

    @Size(max = 255)
    private String homeHeroTitle;

    @Size(max = 500)
    private String homeHeroSubtitle;

    public String getBrandName() {
        return brandName;
    }

    public String getTagline() {
        return tagline;
    }

    public String getPrimaryColor() {
        return primaryColor;
    }

    public String getSecondaryColor() {
        return secondaryColor;
    }

    public String getAccentColor() {
        return accentColor;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public String getBannerUrl() {
        return bannerUrl;
    }

    public String getAppIconUrl() {
        return appIconUrl;
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public Integer getBorderRadius() {
        return borderRadius;
    }

    public Boolean getDarkMode() {
        return darkMode;
    }

    public String getHomeHeroTitle() {
        return homeHeroTitle;
    }

    public String getHomeHeroSubtitle() {
        return homeHeroSubtitle;
    }
}
