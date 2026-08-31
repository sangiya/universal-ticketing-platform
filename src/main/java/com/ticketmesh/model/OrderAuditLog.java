package com.ticketmesh.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Enterprise audit trail for order status transitions.
 * Records every change to an order's state for compliance and debugging.
 */
@Entity
@Table(name = "order_audit_logs")
public class OrderAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private ProductOrder order;

    @Column(nullable = false, length = 20)
    private String fromStatus;

    @Column(nullable = false, length = 20)
    private String toStatus;

    @Column(nullable = false)
    private String reason;

    @Column(nullable = false)
    private String changedBy;

    @Column(nullable = false)
    private Instant timestamp;

    public OrderAuditLog() {}

    public OrderAuditLog(ProductOrder order, String fromStatus, String toStatus, String reason, String changedBy) {
        this.order = order;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.reason = reason;
        this.changedBy = changedBy;
        this.timestamp = Instant.now();
    }

    public Long getId() { return id; }
    public ProductOrder getOrder() { return order; }
    public String getFromStatus() { return fromStatus; }
    public String getToStatus() { return toStatus; }
    public String getReason() { return reason; }
    public String getChangedBy() { return changedBy; }
    public Instant getTimestamp() { return timestamp; }
}
