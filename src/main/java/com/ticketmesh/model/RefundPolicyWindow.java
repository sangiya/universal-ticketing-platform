package com.ticketmesh.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A single refund window in a provider product's cancellation policy.
 *
 * <p>A window applies when the cancellation happens at least {@code minHoursBeforeEvent}
 * hours before the event. Within the window the customer receives
 * {@code refundPercent} of what they paid, less a flat {@code feeAmount} or a
 * percentage {@code feePercent} of the paid total.
 *
 * <p>Windows are evaluated from the most generous to the least: the first window
 * whose lower bound is satisfied wins.
 */
@Entity
@Table(name = "refund_policy_windows")
public class RefundPolicyWindow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "min_hours_before_event", nullable = false)
    private int minHoursBeforeEvent;

    @Column(name = "refund_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal refundPercent;

    @Column(name = "fee_amount", precision = 12, scale = 2)
    private BigDecimal feeAmount;

    @Column(name = "fee_percent", precision = 5, scale = 2)
    private BigDecimal feePercent;

    @Column(name = "label", length = 120)
    private String label;

    public RefundPolicyWindow() {
    }

    public RefundPolicyWindow(Long productId, int minHoursBeforeEvent, BigDecimal refundPercent,
                              BigDecimal feeAmount, BigDecimal feePercent, String label) {
        this.productId = productId;
        this.minHoursBeforeEvent = minHoursBeforeEvent;
        this.refundPercent = refundPercent;
        this.feeAmount = feeAmount;
        this.feePercent = feePercent;
        this.label = label;
    }

    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public int getMinHoursBeforeEvent() {
        return minHoursBeforeEvent;
    }

    public BigDecimal getRefundPercent() {
        return refundPercent;
    }

    public BigDecimal getFeeAmount() {
        return feeAmount;
    }

    public BigDecimal getFeePercent() {
        return feePercent;
    }

    public String getLabel() {
        return label;
    }

    /**
     * The instant at which this window stops applying.
     */
    public Instant expiresAt(Instant eventStart) {
        return eventStart.minusSeconds(minHoursBeforeEvent * 3600L);
    }
}
