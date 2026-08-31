package com.ticketmesh.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "wallet_transactions")
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "wallet_id", nullable = false)
    private Long walletId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Type type;

    @Column(length = 255)
    private String reason;

    @Column(name = "order_ref", length = 40)
    private String orderRef;

    @Column(nullable = false)
    private Instant createdAt;

    protected WalletTransaction() {}

    public WalletTransaction(Long walletId, BigDecimal amount, Type type, String reason, String orderRef) {
        this.walletId = walletId;
        this.amount = amount;
        this.type = type;
        this.reason = reason;
        this.orderRef = orderRef;
        this.createdAt = Instant.now();
    }

    public enum Type { CREDIT, DEBIT, REFUND, ADJUSTMENT }
}
