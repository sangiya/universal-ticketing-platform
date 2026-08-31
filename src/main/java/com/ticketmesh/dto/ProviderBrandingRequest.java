package com.ticketmesh.dto;

import jakarta.validation.constraints.Size;

public class ProviderBrandingRequest {

    @Size(max = 500)
    private String logoUrl;

    @Size(max = 16)
    private String themeColor;

    @Size(max = 16)
    private String secondaryColor;

    @Size(max = 255)
    private String tagline;

    @Size(max = 500)
    private String bannerUrl;

    public String getLogoUrl() {
        return logoUrl;
    }

    public String getThemeColor() {
        return themeColor;
    }

    public String getSecondaryColor() {
        return secondaryColor;
    }

    public String getTagline() {
        return tagline;
    }

    public String getBannerUrl() {
        return bannerUrl;
    }
}
