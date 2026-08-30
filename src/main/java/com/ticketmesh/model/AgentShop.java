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
 * An agent's (shop owner's) business profile. Any shop can connect its service
 * through the app (like Uber/PickMe), upload ticket details and services, and
 * sell to customers once approved by the platform admin.
 */
@Entity
@Table(name = "agent_shops")
public class AgentShop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @NotBlank
    @Size(max = 160)
    @Column(nullable = false, length = 160)
    private String shopName;

    @NotBlank
    @Size(max = 80)
    @Column(nullable = false, length = 80)
    private String businessType;

    @NotBlank
    @Size(min = 2, max = 2)
    @Column(nullable = false, length = 2)
    private String countryIso;

    @NotBlank
    @Size(min = 3, max = 3)
    @Column(nullable = false, length = 3)
    private String currencyIso;

    @Size(max = 500)
    @Column(length = 500)
    private String about;

    @Size(max = 160)
    @Column(length = 160)
    private String contactEmail;

    @Size(max = 40)
    @Column(length = 40)
    private String contactPhone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(nullable = false, updatable = false)
    private Instant appliedAt;

    @Column
    private Instant reviewedAt;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    public AgentShop() {
    }

    public AgentShop(User owner, Tenant tenant, String shopName, String businessType,
                     String countryIso, String currencyIso, String about,
                     String contactEmail, String contactPhone) {
        this.owner = owner;
        this.tenant = tenant;
        this.shopName = shopName;
        this.businessType = businessType;
        this.countryIso = countryIso;
        this.currencyIso = currencyIso;
        this.about = about;
        this.contactEmail = contactEmail;
        this.contactPhone = contactPhone;
        this.status = Status.PENDING;
        this.appliedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public User getOwner() {
        return owner;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
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

    public String getAbout() {
        return about;
    }

    public void setAbout(String about) {
        this.about = about;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Instant getAppliedAt() {
        return appliedAt;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public Long getReviewedBy() {
        return reviewedBy;
    }

    public void approve(Long reviewerId) {
        this.status = Status.APPROVED;
        this.reviewedAt = Instant.now();
        this.reviewedBy = reviewerId;
    }

    public void suspend(Long reviewerId) {
        this.status = Status.SUSPENDED;
        this.reviewedAt = Instant.now();
        this.reviewedBy = reviewerId;
    }

    public enum Status {
        PENDING,
        APPROVED,
        SUSPENDED
    }
}
