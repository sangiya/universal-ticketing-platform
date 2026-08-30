package com.ticketmesh.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * A family group within a tenant: a named group owned by one member with a
 * shared membership used for multi-ticket purchases and shared benefits.
 */
@Entity
@Table(name = "family_groups")
public class FamilyGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String name;

    @NotNull
    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public FamilyGroup() {
    }

    public FamilyGroup(Long tenantId, String name, Long ownerUserId) {
        this.tenantId = tenantId;
        this.name = name;
        this.ownerUserId = ownerUserId;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public String getName() {
        return name;
    }

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
