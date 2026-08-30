package com.ticketmesh.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * A customer review of a product/service, contributing to rating analytics.
 */
@Entity
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "product_id")
    private Long productId;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Min(1)
    @Max(5)
    @Column(nullable = false)
    private int rating;

    @Size(max = 160)
    @Column(length = 160)
    private String title;

    @Size(max = 1000)
    @Column(length = 1000)
    private String comment;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public Review() {
    }

    public Review(Long tenantId, Long productId, Long userId, int rating, String title,
                  String comment) {
        this.tenantId = tenantId;
        this.productId = productId;
        this.userId = userId;
        this.rating = rating;
        this.title = title;
        this.comment = comment;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public Long getProductId() {
        return productId;
    }

    public Long getUserId() {
        return userId;
    }

    public int getRating() {
        return rating;
    }

    public String getTitle() {
        return title;
    }

    public String getComment() {
        return comment;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
