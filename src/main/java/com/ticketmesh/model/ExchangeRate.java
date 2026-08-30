package com.ticketmesh.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A configurable currency exchange rate (base -> target). Enables a truly
 * global, multi-currency product: any country / any currency.
 */
@Entity
@Table(name = "exchange_rates")
public class ExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id")
    private Long tenantId;

    @NotNull
    @Size(min = 3, max = 3)
    @Column(name = "base_currency", nullable = false, length = 3)
    private String baseCurrency;

    @NotNull
    @Size(min = 3, max = 3)
    @Column(name = "target_currency", nullable = false, length = 3)
    private String targetCurrency;

    @Positive
    @NotNull
    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal rate;

    @Column(nullable = false)
    private Instant updatedAt;

    public ExchangeRate() {
    }

    public ExchangeRate(Long tenantId, String baseCurrency, String targetCurrency, BigDecimal rate) {
        this.tenantId = tenantId;
        this.baseCurrency = baseCurrency;
        this.targetCurrency = targetCurrency;
        this.rate = rate;
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public String getTargetCurrency() {
        return targetCurrency;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
        this.updatedAt = Instant.now();
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
