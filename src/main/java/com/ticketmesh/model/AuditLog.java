package com.ticketmesh.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * Immutable, high-security audit trail of sensitive platform actions. Every
 * admin/agent/security action is recorded for accountability.
 */
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Size(max = 50)
    @Column(name = "actor_username", length = 50)
    private String actorUsername;

    @NotBlank
    @Size(max = 80)
    @Column(nullable = false, length = 80)
    private String action;

    @Size(max = 60)
    @Column(name = "resource_type", length = 60)
    private String resourceType;

    @Size(max = 120)
    @Column(name = "resource_ref", length = 120)
    private String resourceRef;

    @Size(max = 1000)
    @Column(length = 1000)
    private String detail;

    @Size(max = 45)
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public AuditLog() {
    }

    public AuditLog(Long tenantId, String actorUsername, String action, String resourceType,
                    String resourceRef, String detail, String ipAddress) {
        this.tenantId = tenantId;
        this.actorUsername = actorUsername;
        this.action = action;
        this.resourceType = resourceType;
        this.resourceRef = resourceRef;
        this.detail = detail;
        this.ipAddress = ipAddress;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public String getAction() {
        return action;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getResourceRef() {
        return resourceRef;
    }

    public String getDetail() {
        return detail;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
