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
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * Identity verification for agents (NIC / passport / driving licence) with a
 * captured photo — the stricter onboarding gate most consumer apps require.
 */
@Entity
@Table(name = "identity_verifications")
public class IdentityVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotBlank
    @Enumerated(EnumType.STRING)
    @Column(name = "doc_type", nullable = false, length = 20)
    private DocumentType documentType;

    @NotBlank
    @Size(max = 64)
    @Column(name = "doc_number", nullable = false, length = 64)
    private String documentNumber;

    @Size(max = 500)
    @Column(name = "doc_photo_url", length = 500)
    private String documentPhotoUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(name = "verified_by")
    private Long verifiedBy;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public IdentityVerification() {
    }

    public IdentityVerification(User user, DocumentType documentType, String documentNumber,
                                String documentPhotoUrl) {
        this.user = user;
        this.documentType = documentType;
        this.documentNumber = documentNumber;
        this.documentPhotoUrl = documentPhotoUrl;
        this.status = Status.PENDING;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public String getDocumentPhotoUrl() {
        return documentPhotoUrl;
    }

    public Status getStatus() {
        return status;
    }

    public void markApproved(Long verifierId) {
        this.status = Status.APPROVED;
        this.verifiedBy = verifierId;
        this.verifiedAt = Instant.now();
    }

    public void markRejected() {
        this.status = Status.REJECTED;
    }

    public Long getVerifiedBy() {
        return verifiedBy;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public enum DocumentType {
        NIC,
        PASSPORT,
        DRIVING_LICENCE
    }

    public enum Status {
        PENDING,
        APPROVED,
        REJECTED
    }
}
