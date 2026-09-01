package com.ticketmesh.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank
    @Size(min = 3, max = 50)
    private String username;

    @NotBlank
    @Size(min = 8, max = 100)
    private String password;

    @NotBlank
    @Size(max = 120)
    private String fullName;

    @NotBlank
    @Email
    @Size(max = 160)
    private String email;

    private String role;

    @Size(max = 60)
    private String tenantSlug;

    @Size(max = 40)
    private String phone;

    @Size(min = 2, max = 2)
    private String countryIso;

    @Size(min = 3, max = 3)
    private String currencyIso;

    @Size(max = 8)
    private String defaultLanguage;

    @Size(max = 64)
    private String timezone;

    @Valid
    private KycPayload kyc;

    @Valid
    private BusinessPayload business;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getTenantSlug() { return tenantSlug; }
    public void setTenantSlug(String tenantSlug) { this.tenantSlug = tenantSlug; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getCountryIso() { return countryIso; }
    public void setCountryIso(String countryIso) { this.countryIso = countryIso; }

    public String getCurrencyIso() { return currencyIso; }
    public void setCurrencyIso(String currencyIso) { this.currencyIso = currencyIso; }

    public String getDefaultLanguage() { return defaultLanguage; }
    public void setDefaultLanguage(String defaultLanguage) { this.defaultLanguage = defaultLanguage; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }

    public KycPayload getKyc() { return kyc; }
    public void setKyc(KycPayload kyc) { this.kyc = kyc; }

    public BusinessPayload getBusiness() { return business; }
    public void setBusiness(BusinessPayload business) { this.business = business; }

    // ── Nested payloads ───────────────────────────────────────────

    public static class KycPayload {
        @Size(max = 20)
        private String idType;     // NIC | PASSPORT | DRIVING_LICENSE
        @Size(max = 60)
        private String idNumber;
        @Size(max = 20)
        private String dateOfBirth;
        @Size(max = 20)
        private String gender;     // MALE | FEMALE | OTHER
        @Valid
        private AddressPayload address;
        private boolean kycConsent;

        public String getIdType() { return idType; }
        public void setIdType(String idType) { this.idType = idType; }
        public String getIdNumber() { return idNumber; }
        public void setIdNumber(String idNumber) { this.idNumber = idNumber; }
        public String getDateOfBirth() { return dateOfBirth; }
        public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }
        public String getGender() { return gender; }
        public void setGender(String gender) { this.gender = gender; }
        public AddressPayload getAddress() { return address; }
        public void setAddress(AddressPayload address) { this.address = address; }
        public boolean isKycConsent() { return kycConsent; }
        public void setKycConsent(boolean kycConsent) { this.kycConsent = kycConsent; }
    }

    public static class AddressPayload {
        @Size(max = 200)
        private String line1;
        @Size(max = 200)
        private String line2;
        @Size(max = 100)
        private String city;
        @Size(max = 100)
        private String state;
        @Size(max = 20)
        private String postalCode;
        @Size(min = 2, max = 2)
        private String countryIso;

        public String getLine1() { return line1; }
        public void setLine1(String line1) { this.line1 = line1; }
        public String getLine2() { return line2; }
        public void setLine2(String line2) { this.line2 = line2; }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        public String getState() { return state; }
        public void setState(String state) { this.state = state; }
        public String getPostalCode() { return postalCode; }
        public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
        public String getCountryIso() { return countryIso; }
        public void setCountryIso(String countryIso) { this.countryIso = countryIso; }
    }

    public static class BusinessPayload {
        @NotBlank
        @Size(max = 160)
        private String name;
        @NotBlank
        @Size(max = 60)
        private String registrationNumber;
        @Size(max = 60)
        private String taxId;
        @Size(max = 20)
        private String type;       // SOLE | PARTNERSHIP | COMPANY | NGO
        @NotBlank
        @Size(max = 400)
        private String address;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getRegistrationNumber() { return registrationNumber; }
        public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }
        public String getTaxId() { return taxId; }
        public void setTaxId(String taxId) { this.taxId = taxId; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
    }
}
