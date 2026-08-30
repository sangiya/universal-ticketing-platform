package com.ticketmesh.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * A flagged fraud or risk signal produced by the fraud-detection and
 * auto-issue-detection system. Signals carry a normalized 0-100 risk score and a
 * set of named flags (e.g. HIGH_VELOCITY, ODD_AMOUNT, REPEATED_ATTEMPT) so the
 * platform (and support team) can act on suspicious behaviour.
 */
@Entity
@Table(name = "fraud_signals")
public class FraudSignal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Size(max = 20)
    @Column(nullable = false, length = 20)
    private String subjectType;

    @Size(max = 60)
    @Column(nullable = false, length = 60)
    private String subjectRef;

    @Size(max = 50)
    @Column(length = 50)
    private String actorUsername;

    @Column(nullable = false)
    private int score;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Risk risk;

    @Size(max = 500)
    @Column(length = 500)
    private String flags;

    @Column(columnDefinition = "CLOB")
    private String details;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public FraudSignal() {
    }

    public FraudSignal(Long tenantId, String subjectType, String subjectRef,
                       String actorUsername, int score, Risk risk,
                       String flags, String details) {
        this.tenantId = tenantId;
        this.subjectType = subjectType;
        this.subjectRef = subjectRef;
        this.actorUsername = actorUsername;
        this.score = score;
        this.risk = risk;
        this.flags = flags;
        this.details = details;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public String getSubjectType() {
        return subjectType;
    }

    public String getSubjectRef() {
        return subjectRef;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public int getScore() {
        return score;
    }

    public Risk getRisk() {
        return risk;
    }

    public String getFlags() {
        return flags;
    }

    public String getDetails() {
        return details;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public enum Risk {
        LOW,
        MEDIUM,
        HIGH
    }
}
