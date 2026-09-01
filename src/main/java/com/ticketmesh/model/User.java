package com.ticketmesh.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String fullName;

    @NotBlank
    @Email
    @Size(max = 160)
    @Column(nullable = false, unique = true, length = 160)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Status status;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Size(max = 40)
    @Column(length = 40)
    private String phone;

    @Column(name = "email_encrypted", length = 512)
    private String emailEncrypted;

    @Column(name = "phone_encrypted", length = 512)
    private String phoneEncrypted;

    @Enumerated(EnumType.STRING)
    @Column(name = "two_factor_method", length = 20)
    private TwoFactorMethod twoFactorMethod;

    @Column(name = "totp_secret", length = 128)
    private String totpSecret;

    @Column(name = "totp_enabled", nullable = false)
    private boolean totpEnabled = false;

    @Column(name = "app_key_hash", length = 255)
    private String appKeyHash;

    @Column(name = "app_key_issued_at")
    private Instant appKeyIssuedAt;

    // ── KYC fields (identity & address verification) ──

    @Size(max = 20)
    @Column(name = "kyc_id_type", length = 20)
    private String kycIdType;     // NIC | PASSPORT | DRIVING_LICENSE

    @Size(max = 60)
    @Column(name = "kyc_id_number", length = 60)
    private String kycIdNumber;

    @Size(max = 20)
    @Column(name = "kyc_date_of_birth", length = 20)
    private String kycDateOfBirth;

    @Size(max = 20)
    @Column(name = "kyc_gender", length = 20)
    private String kycGender;

    @Size(max = 200)
    @Column(name = "kyc_address_line1", length = 200)
    private String kycAddressLine1;

    @Size(max = 200)
    @Column(name = "kyc_address_line2", length = 200)
    private String kycAddressLine2;

    @Size(max = 100)
    @Column(name = "kyc_city", length = 100)
    private String kycCity;

    @Size(max = 100)
    @Column(name = "kyc_state", length = 100)
    private String kycState;

    @Size(max = 20)
    @Column(name = "kyc_postal_code", length = 20)
    private String kycPostalCode;

    @Size(min = 2, max = 2)
    @Column(name = "kyc_country_iso", length = 2)
    private String kycCountryIso;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", length = 20)
    private KycStatus kycStatus;

    @Column(name = "kyc_verified_at")
    private Instant kycVerifiedAt;

    // ── Agent business KYC ──

    @Size(max = 160)
    @Column(name = "biz_name", length = 160)
    private String businessName;

    @Size(max = 60)
    @Column(name = "biz_reg_number", length = 60)
    private String businessRegNumber;

    @Size(max = 60)
    @Column(name = "biz_tax_id", length = 60)
    private String businessTaxId;

    @Size(max = 20)
    @Column(name = "biz_type", length = 20)
    private String businessType;   // SOLE | PARTNERSHIP | COMPANY | NGO

    @Size(max = 400)
    @Column(name = "biz_address", length = 400)
    private String businessAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "biz_status", length = 20)
    private KycStatus businessStatus;

    @Column(name = "biz_verified_at")
    private Instant businessVerifiedAt;

    public User() {
    }

    public User(String username, String password, String fullName, String email, Role role) {
        this(username, password, fullName, email, role, null);
    }

    public User(String username, String password, String fullName, String email, Role role,
                Long tenantId) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.tenantId = tenantId;
        this.status = Status.ACTIVE;
        this.createdAt = Instant.now();
    }

    public User(String username, String password, String fullName, String email, Role role,
                Long tenantId, String phone) {
        this(username, password, fullName, email, role, tenantId);
        this.phone = phone;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmailEncrypted() {
        return emailEncrypted;
    }

    public void setEmailEncrypted(String emailEncrypted) {
        this.emailEncrypted = emailEncrypted;
    }

    public String getPhoneEncrypted() {
        return phoneEncrypted;
    }

    public void setPhoneEncrypted(String phoneEncrypted) {
        this.phoneEncrypted = phoneEncrypted;
    }

    public TwoFactorMethod getTwoFactorMethod() {
        return twoFactorMethod;
    }

    public void setTwoFactorMethod(TwoFactorMethod twoFactorMethod) {
        this.twoFactorMethod = twoFactorMethod;
    }

    public String getTotpSecret() {
        return totpSecret;
    }

    public void setTotpSecret(String totpSecret) {
        this.totpSecret = totpSecret;
    }

    public boolean isTotpEnabled() {
        return totpEnabled;
    }

    public void setTotpEnabled(boolean totpEnabled) {
        this.totpEnabled = totpEnabled;
    }

    public String getAppKeyHash() {
        return appKeyHash;
    }

    public void setAppKeyHash(String appKeyHash) {
        this.appKeyHash = appKeyHash;
    }

    public Instant getAppKeyIssuedAt() {
        return appKeyIssuedAt;
    }

    public void setAppKeyIssuedAt(Instant appKeyIssuedAt) {
        this.appKeyIssuedAt = appKeyIssuedAt;
    }

    public enum Role {
        CUSTOMER,
        AGENT,
        ADMIN
    }

    public enum Status {
        ACTIVE,
        SUSPENDED
    }

    public enum TwoFactorMethod {
        OTP_EMAIL,
        OTP_SMS,
        TOTP,
        APP_KEY
    }

    public enum KycStatus {
        PENDING,
        SUBMITTED,
        VERIFIED,
        REJECTED
    }

    // ── Getters and setters for KYC fields ──

    public String getKycIdType() { return kycIdType; }
    public void setKycIdType(String kycIdType) { this.kycIdType = kycIdType; }

    public String getKycIdNumber() { return kycIdNumber; }
    public void setKycIdNumber(String kycIdNumber) { this.kycIdNumber = kycIdNumber; }

    public String getKycDateOfBirth() { return kycDateOfBirth; }
    public void setKycDateOfBirth(String kycDateOfBirth) { this.kycDateOfBirth = kycDateOfBirth; }

    public String getKycGender() { return kycGender; }
    public void setKycGender(String kycGender) { this.kycGender = kycGender; }

    public String getKycAddressLine1() { return kycAddressLine1; }
    public void setKycAddressLine1(String kycAddressLine1) { this.kycAddressLine1 = kycAddressLine1; }

    public String getKycAddressLine2() { return kycAddressLine2; }
    public void setKycAddressLine2(String kycAddressLine2) { this.kycAddressLine2 = kycAddressLine2; }

    public String getKycCity() { return kycCity; }
    public void setKycCity(String kycCity) { this.kycCity = kycCity; }

    public String getKycState() { return kycState; }
    public void setKycState(String kycState) { this.kycState = kycState; }

    public String getKycPostalCode() { return kycPostalCode; }
    public void setKycPostalCode(String kycPostalCode) { this.kycPostalCode = kycPostalCode; }

    public String getKycCountryIso() { return kycCountryIso; }
    public void setKycCountryIso(String kycCountryIso) { this.kycCountryIso = kycCountryIso; }

    public KycStatus getKycStatus() { return kycStatus; }
    public void setKycStatus(KycStatus kycStatus) { this.kycStatus = kycStatus; }

    public Instant getKycVerifiedAt() { return kycVerifiedAt; }
    public void setKycVerifiedAt(Instant kycVerifiedAt) { this.kycVerifiedAt = kycVerifiedAt; }

    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }

    public String getBusinessRegNumber() { return businessRegNumber; }
    public void setBusinessRegNumber(String businessRegNumber) { this.businessRegNumber = businessRegNumber; }

    public String getBusinessTaxId() { return businessTaxId; }
    public void setBusinessTaxId(String businessTaxId) { this.businessTaxId = businessTaxId; }

    public String getBusinessType() { return businessType; }
    public void setBusinessType(String businessType) { this.businessType = businessType; }

    public String getBusinessAddress() { return businessAddress; }
    public void setBusinessAddress(String businessAddress) { this.businessAddress = businessAddress; }

    public KycStatus getBusinessStatus() { return businessStatus; }
    public void setBusinessStatus(KycStatus businessStatus) { this.businessStatus = businessStatus; }

    public Instant getBusinessVerifiedAt() { return businessVerifiedAt; }
    public void setBusinessVerifiedAt(Instant businessVerifiedAt) { this.businessVerifiedAt = businessVerifiedAt; }
}
