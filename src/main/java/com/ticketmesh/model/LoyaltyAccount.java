package com.ticketmesh.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * A customer loyalty account: earn points on bookings, progress through tiers.
 */
@Entity
@Table(name = "loyalty_accounts")
public class LoyaltyAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private long points;

    @Column(name = "lifetime_points", nullable = false)
    private long lifetimePoints;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Tier tier;

    @Column(nullable = false)
    private Instant updatedAt;

    public LoyaltyAccount() {
    }

    public LoyaltyAccount(Long tenantId, Long userId) {
        this.tenantId = tenantId;
        this.userId = userId;
        this.points = 0;
        this.lifetimePoints = 0;
        this.tier = Tier.BRONZE;
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public Long getUserId() {
        return userId;
    }

    public long getPoints() {
        return points;
    }

    public long getLifetimePoints() {
        return lifetimePoints;
    }

    public Tier getTier() {
        return tier;
    }

    public void earn(long awarded) {
        this.points = this.points + awarded;
        this.lifetimePoints = this.lifetimePoints + awarded;
        recomputeTier();
        this.updatedAt = Instant.now();
    }

    public boolean redeem(long amount) {
        if (amount < 0 || amount > this.points) {
            return false;
        }
        this.points = this.points - amount;
        this.updatedAt = Instant.now();
        return true;
    }

    /**
     * Claw back points awarded by a cancelled or refunded order.
     *
     * <p>Only the points the customer can actually give up are taken: if the
     * balance has already been spent, the account floors at zero instead of
     * going negative. Lifetime points are never decremented, so an honest
     * reversal cannot demote a customer's earned tier.
     *
     * @return the number of points actually removed
     */
    public long reverse(long amount) {
        if (amount <= 0) {
            return 0;
        }
        long removed = Math.min(amount, this.points);
        this.points = this.points - removed;
        this.updatedAt = Instant.now();
        return removed;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private void recomputeTier() {
        if (lifetimePoints >= 10000) {
            this.tier = Tier.PLATINUM;
        } else if (lifetimePoints >= 5000) {
            this.tier = Tier.GOLD;
        } else if (lifetimePoints >= 1500) {
            this.tier = Tier.SILVER;
        } else {
            this.tier = Tier.BRONZE;
        }
    }

    public enum Tier {
        BRONZE,
        SILVER,
        GOLD,
        PLATINUM
    }
}
