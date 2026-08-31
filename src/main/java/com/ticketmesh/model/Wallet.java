package com.ticketmesh.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "wallets")
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "currency_iso", nullable = false, length = 3)
    private String currencyIso = "LKR";

    @Column(nullable = false)
    private Instant updatedAt;

    protected Wallet() {}

    public Wallet(Long tenantId, Long userId, String currencyIso) {
        this.tenantId = tenantId;
        this.userId = userId;
        this.currencyIso = currencyIso;
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getTenantId() { return tenantId; }
    public Long getUserId() { return userId; }
    public BigDecimal getBalance() { return balance; }
    public String getCurrencyIso() { return currencyIso; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void credit(BigDecimal amount) {
        this.balance = this.balance.add(amount);
        this.updatedAt = Instant.now();
    }

    public boolean debit(BigDecimal amount) {
        if (balance.compareTo(amount) < 0) return false;
        this.balance = this.balance.subtract(amount);
        this.updatedAt = Instant.now();
        return true;
    }
}
