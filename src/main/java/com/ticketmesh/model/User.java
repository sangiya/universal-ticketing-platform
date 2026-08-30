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
}
