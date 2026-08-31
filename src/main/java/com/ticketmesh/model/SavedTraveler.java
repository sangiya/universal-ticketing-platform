package com.ticketmesh.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "saved_travelers",
       uniqueConstraints = @UniqueConstraint(columnNames = {"owner_user_id", "full_name", "date_of_birth"}))
public class SavedTraveler {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    @Column(nullable = false, length = 120)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Relationship relationship = Relationship.OTHER;

    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Gender gender = Gender.UNSPECIFIED;

    @Column(length = 2)
    private String nationality;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private DocumentType documentType;

    @Column(name = "document_number_enc", length = 500)
    private String documentNumberEnc;

    @Column(name = "phone_enc", length = 500)
    private String phoneEnc;

    @Column(name = "email_enc", length = 500)
    private String emailEnc;

    @Column(nullable = false)
    private boolean consentGiven;

    private Instant consentAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected SavedTraveler() {}

    public SavedTraveler(Long tenantId, Long ownerUserId, String fullName,
                         Relationship relationship, LocalDate dateOfBirth,
                         Gender gender, String nationality,
                         DocumentType documentType, String documentNumberEnc,
                         String phoneEnc, String emailEnc, boolean consentGiven) {
        this.tenantId = tenantId;
        this.ownerUserId = ownerUserId;
        this.fullName = fullName;
        this.relationship = relationship;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.nationality = nationality;
        this.documentType = documentType;
        this.documentNumberEnc = documentNumberEnc;
        this.phoneEnc = phoneEnc;
        this.emailEnc = emailEnc;
        this.consentGiven = consentGiven;
        this.consentAt = consentGiven ? Instant.now() : null;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getTenantId() { return tenantId; }
    public Long getOwnerUserId() { return ownerUserId; }
    public String getFullName() { return fullName; }
    public void setFullName(String v) { this.fullName = v; touch(); }
    public Relationship getRelationship() { return relationship; }
    public void setRelationship(Relationship v) { this.relationship = v; touch(); }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate v) { this.dateOfBirth = v; touch(); }
    public Gender getGender() { return gender; }
    public void setGender(Gender v) { this.gender = v; touch(); }
    public String getNationality() { return nationality; }
    public void setNationality(String v) { this.nationality = v; touch(); }
    public DocumentType getDocumentType() { return documentType; }
    public void setDocumentType(DocumentType v) { this.documentType = v; touch(); }
    public String getDocumentNumberEnc() { return documentNumberEnc; }
    public void setDocumentNumberEnc(String v) { this.documentNumberEnc = v; touch(); }
    public String getPhoneEnc() { return phoneEnc; }
    public void setPhoneEnc(String v) { this.phoneEnc = v; touch(); }
    public String getEmailEnc() { return emailEnc; }
    public void setEmailEnc(String v) { this.emailEnc = v; touch(); }
    public boolean isConsentGiven() { return consentGiven; }
    public Instant getConsentAt() { return consentAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    private void touch() { this.updatedAt = Instant.now(); }

    public enum Relationship { SELF, SPOUSE, CHILD, PARENT, SIBLING, OTHER }
    public enum Gender { MALE, FEMALE, OTHER, UNSPECIFIED }
    public enum DocumentType { PASSPORT, NIC, DRIVING_LICENSE, OTHER }
}
