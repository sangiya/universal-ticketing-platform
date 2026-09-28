package com.ticketmesh.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "loyalty_ledger")
public class LoyaltyLedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "points_delta", nullable = false)
    private long pointsDelta;

    @Column(name = "balance_after", nullable = false)
    private long balanceAfter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EntryType type;

    @Column(length = 255)
    private String reason;

    @Column(name = "order_ref", length = 40)
    private String orderRef;

    @Column(nullable = false)
    private Instant createdAt;

    protected LoyaltyLedgerEntry() {}

    public LoyaltyLedgerEntry(Long tenantId, Long userId, long pointsDelta, long balanceAfter,
                               EntryType type, String reason, String orderRef) {
        this.tenantId = tenantId;
        this.userId = userId;
        this.pointsDelta = pointsDelta;
        this.balanceAfter = balanceAfter;
        this.type = type;
        this.reason = reason;
        this.orderRef = orderRef;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getTenantId() { return tenantId; }
    public Long getUserId() { return userId; }
    public long getPointsDelta() { return pointsDelta; }
    public long getBalanceAfter() { return balanceAfter; }
    public EntryType getType() { return type; }
    public String getReason() { return reason; }
    public String getOrderRef() { return orderRef; }
    public Instant getCreatedAt() { return createdAt; }

    public enum EntryType {
        ACCRUAL,
        REDEMPTION,
        EXPIRATION,
        ADJUSTMENT,
        WELCOME_BONUS,
        /** Points clawed back when an order is cancelled or refunded. */
        REVERSAL
    }
}
