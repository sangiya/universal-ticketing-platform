package com.ticketmesh.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TenantRequest {

    @NotBlank
    @Size(min = 2, max = 60)
    private String slug;

    @NotBlank
    @Size(max = 160)
    private String name;

    @NotBlank
    @Size(min = 2, max = 2)
    private String countryIso;

    @NotBlank
    @Size(min = 3, max = 3)
    private String currencyIso;

    @NotBlank
    @Size(max = 8)
    private String defaultLanguage;

    @NotBlank
    @Size(max = 64)
    private String timezone;

    @Size(max = 160)
    private String domain;

    public String getSlug() {
        return slug;
    }

    public String getName() {
        return name;
    }

    public String getCountryIso() {
        return countryIso;
    }

    public String getCurrencyIso() {
        return currencyIso;
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public String getTimezone() {
        return timezone;
    }

    public String getDomain() {
        return domain;
    }
}
