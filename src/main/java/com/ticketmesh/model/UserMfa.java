package com.ticketmesh.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "user_mfa", uniqueConstraints = @UniqueConstraint(columnNames = "user_id"))
public class UserMfa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "secret_enc", length = 500)
    private String secretEnc;

    @Column(nullable = false)
    private boolean enabled = false;

    @Column(name = "backup_codes_enc", length = 2000)
    private String backupCodesEnc;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected UserMfa() {}

    public UserMfa(Long userId) {
        this.userId = userId;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getSecretEnc() { return secretEnc; }
    public void setSecretEnc(String v) { this.secretEnc = v; touch(); }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean v) { this.enabled = v; touch(); }
    public String getBackupCodesEnc() { return backupCodesEnc; }
    public void setBackupCodesEnc(String v) { this.backupCodesEnc = v; touch(); }
    public Instant getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(Instant v) { this.verifiedAt = v; touch(); }
    private void touch() { this.updatedAt = Instant.now(); }
}
