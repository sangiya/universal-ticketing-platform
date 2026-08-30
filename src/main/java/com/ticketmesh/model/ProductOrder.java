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
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A universal marketplace order: a customer buys any provider product through
 * the universal booking engine. Captures the full price breakdown (base, tax,
 * service fee, discount) and supports promotions and multi-currency.
 */
@Entity
@Table(name = "product_orders")
public class ProductOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 40)
    @Column(nullable = false, unique = true, length = 40)
    private String orderRef;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProviderProduct product;

    @Size(max = 160)
    @Column(name = "provider_name", length = 160)
    private String providerName;

    @NotBlank
    @Size(max = 200)
    @Column(name = "product_title", nullable = false, length = 200)
    private String productTitle;

    @NotBlank
    @Size(max = 30)
    @Column(name = "product_type", nullable = false, length = 30)
    private String productType;

    @Positive
    @Column(nullable = false)
    private int quantity;

    @NotNull
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @NotBlank
    @Size(min = 3, max = 3)
    @Column(name = "currency_iso", nullable = false, length = 3)
    private String currencyIso;

    @NotNull
    @Column(name = "base_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal baseAmount;

    @NotNull
    @Column(name = "tax_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal taxAmount;

    @NotNull
    @Column(name = "service_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal serviceFee;

    @NotNull
    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @NotNull
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Size(max = 40)
    @Column(name = "promo_code", length = 40)
    private String promoCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    public ProductOrder() {
    }

    public ProductOrder(String orderRef, Tenant tenant, User user, ProviderProduct product,
                        int quantity, BigDecimal unitPrice, String currencyIso,
                        BigDecimal baseAmount, BigDecimal taxAmount, BigDecimal serviceFee,
                        BigDecimal discountAmount, BigDecimal totalAmount, String promoCode) {
        this.orderRef = orderRef;
        this.tenant = tenant;
        this.user = user;
        this.product = product;
        this.providerName = product.getProvider().getName();
        this.productTitle = product.getTitle();
        this.productType = product.getProductType().name();
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.currencyIso = currencyIso;
        this.baseAmount = baseAmount;
        this.taxAmount = taxAmount;
        this.serviceFee = serviceFee;
        this.discountAmount = discountAmount;
        this.totalAmount = totalAmount;
        this.promoCode = promoCode;
        this.status = Status.CONFIRMED;
        this.createdAt = Instant.now();
        this.paidAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getOrderRef() {
        return orderRef;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public User getUser() {
        return user;
    }

    public ProviderProduct getProduct() {
        return product;
    }

    public String getProviderName() {
        return providerName;
    }

    public String getProductTitle() {
        return productTitle;
    }

    public String getProductType() {
        return productType;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public String getCurrencyIso() {
        return currencyIso;
    }

    public BigDecimal getBaseAmount() {
        return baseAmount;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public BigDecimal getServiceFee() {
        return serviceFee;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getPromoCode() {
        return promoCode;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public enum Status {
        PENDING,
        CONFIRMED,
        CANCELLED,
        REFUNDED
    }
}
