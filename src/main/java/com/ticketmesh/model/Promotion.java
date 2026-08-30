package com.ticketmesh.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A promotion / coupon rule. Configurable by admin per tenant: percentage or
 * flat discount, optional minimum purchase, validity window, usage cap.
 */
@Entity
@Table(name = "promotions")
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @NotBlank
    @Size(max = 40)
    @Column(nullable = false, length = 40)
    private String code;

    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private DiscountType discountType;

    @NotNull
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "min_purchase", precision = 12, scale = 2)
    private BigDecimal minPurchase;

    @Column(name = "starts_at")
    private Instant startsAt;

    @Column(name = "ends_at")
    private Instant endsAt;

    @Column(name = "max_uses")
    private Integer maxUses;

    @Column(nullable = false)
    private int usedCount;

    @Column(length = 255)
    private String domains;

    @Column(nullable = false)
    private boolean enabled = true;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private Kind kind = Kind.PROMO;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public Promotion() {
    }

    public Promotion(Long tenantId, String code, String name, DiscountType discountType,
                     BigDecimal discountValue, BigDecimal minPurchase, Instant startsAt,
                     Instant endsAt, Integer maxUses, String domains, Kind kind) {
        this.tenantId = tenantId;
        this.code = code;
        this.name = name;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.minPurchase = minPurchase;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.maxUses = maxUses;
        this.domains = domains;
        this.kind = kind != null ? kind : Kind.PROMO;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public DiscountType getDiscountType() {
        return discountType;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public BigDecimal getMinPurchase() {
        return minPurchase;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public Integer getMaxUses() {
        return maxUses;
    }

    public int getUsedCount() {
        return usedCount;
    }

    public void incrementUsed() {
        this.usedCount = this.usedCount + 1;
    }

    public String getDomains() {
        return domains;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Kind getKind() {
        return kind;
    }

    public void setKind(Kind kind) {
        this.kind = kind;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isActiveNow() {
        if (!enabled) {
            return false;
        }
        Instant now = Instant.now();
        if (startsAt != null && now.isBefore(startsAt)) {
            return false;
        }
        return endsAt == null || !now.isAfter(endsAt);
    }

    public enum DiscountType {
        PERCENT,
        FLAT,
        VOUCHER,
        OFFER
    }

    public enum Kind {
        PROMO,
        VOUCHER,
        OFFER
    }
}
