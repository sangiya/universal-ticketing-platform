package com.ticketmesh.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "settlement_entries")
public class SettlementEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "order_ref", nullable = false, length = 40)
    private String orderRef;

    @Column(name = "provider_id")
    private Long providerId;

    @Column(name = "product_type", length = 30)
    private String productType;

    @Column(name = "gross_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal grossAmount;

    @Column(name = "platform_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal platformFee;

    @Column(name = "net_payout", nullable = false, precision = 12, scale = 2)
    private BigDecimal netPayout;

    @Column(name = "currency_iso", nullable = false, length = 3)
    private String currencyIso;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(nullable = false)
    private Instant createdAt;

    protected SettlementEntry() {}

    public SettlementEntry(Long tenantId, String orderRef, Long providerId, String productType,
                           BigDecimal grossAmount, BigDecimal platformFee, BigDecimal netPayout,
                           String currencyIso) {
        this.tenantId = tenantId;
        this.orderRef = orderRef;
        this.providerId = providerId;
        this.productType = productType;
        this.grossAmount = grossAmount;
        this.platformFee = platformFee;
        this.netPayout = netPayout;
        this.currencyIso = currencyIso;
        this.status = Status.PENDING_PAYOUT;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getTenantId() { return tenantId; }
    public String getOrderRef() { return orderRef; }
    public BigDecimal getGrossAmount() { return grossAmount; }
    public BigDecimal getPlatformFee() { return platformFee; }
    public BigDecimal getNetPayout() { return netPayout; }
    public String getCurrencyIso() { return currencyIso; }
    public Status getStatus() { return status; }

    /**
     * Hold the payout while a cancellation is being processed.
     */
    public void hold() {
        this.status = Status.HOLD;
    }

    /**
     * Mark the payout as clawed back after a refund (spec section 26).
     */
    public void markRefunded() {
        this.status = Status.REFUNDED;
    }

    public enum Status { PENDING_PAYOUT, PAID_OUT, REFUNDED, HOLD }
}
