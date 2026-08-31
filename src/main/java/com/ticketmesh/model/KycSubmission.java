package com.ticketmesh.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "kyc_submissions")
public class KycSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "shop_id")
    private Long shopId;

    @Column(name = "document_type", length = 30)
    private String documentType;

    @Column(name = "document_number_enc", length = 500)
    private String documentNumberEnc;

    @Column(name = "document_url", length = 500)
    private String documentUrl;

    @Column(name = "selfie_url", length = 500)
    private String selfieUrl;

    @Column(name = "business_reg", length = 100)
    private String businessReg;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private Status status = Status.SUBMITTED;

    @Column(name = "internal_reason", length = 255)
    private String internalReason;

    @Column(name = "customer_reason", length = 255)
    private String customerReason;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant reviewedAt;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    protected KycSubmission() {}

    public KycSubmission(Long tenantId, Long userId, Long shopId, String documentType,
                         String documentNumberEnc, String documentUrl, String selfieUrl, String businessReg) {
        this.tenantId = tenantId;
        this.userId = userId;
        this.shopId = shopId;
        this.documentType = documentType;
        this.documentNumberEnc = documentNumberEnc;
        this.documentUrl = documentUrl;
        this.selfieUrl = selfieUrl;
        this.businessReg = businessReg;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Status getStatus() { return status; }
    public void setStatus(Status s) { this.status = s; }
    public String getDocumentType() { return documentType; }
    public String getDocumentNumberEnc() { return documentNumberEnc; }
    public String getDocumentUrl() { return documentUrl; }
    public String getSelfieUrl() { return selfieUrl; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Instant v) { this.reviewedAt = v; }
    public Long getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(Long v) { this.reviewedBy = v; }
    public String getInternalReason() { return internalReason; }
    public void setInternalReason(String v) { this.internalReason = v; }
    public String getCustomerReason() { return customerReason; }
    public void setCustomerReason(String v) { this.customerReason = v; }

    public enum Status { NOT_STARTED, SUBMITTED, IN_REVIEW, APPROVED, REJECTED, EXPIRED, REQUIRES_MORE_INFO }
}
