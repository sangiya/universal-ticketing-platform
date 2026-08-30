package com.ticketmesh.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ShopApplyRequest {

    @NotBlank
    @Size(max = 160)
    private String shopName;

    @NotBlank
    @Size(max = 80)
    private String businessType;

    @NotBlank
    @Size(min = 2, max = 2)
    private String countryIso;

    @NotBlank
    @Size(min = 3, max = 3)
    private String currencyIso;

    @Size(max = 500)
    private String about;

    @Email
    @Size(max = 160)
    private String contactEmail;

    @Size(max = 40)
    private String contactPhone;

    public String getShopName() {
        return shopName;
    }

    public String getBusinessType() {
        return businessType;
    }

    public String getCountryIso() {
        return countryIso;
    }

    public String getCurrencyIso() {
        return currencyIso;
    }

    public String getAbout() {
        return about;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public String getContactPhone() {
        return contactPhone;
    }
}
