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

import java.time.Instant;

/**
 * A one-time passcode (email / SMS OTP, or 2FA challenge code). Stored as a
 * hash, single-use, time-limited — supports the OTP / two-factor onboarding
 * security requirement.
 */
@Entity
@Table(name = "otp_codes")
public class OtpCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Purpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Channel channel;

    @Column(name = "code_hash", nullable = false, length = 255)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean consumed = false;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public OtpCode() {
    }

    public OtpCode(User user, Purpose purpose, Channel channel, String codeHash, Instant expiresAt) {
        this.user = user;
        this.purpose = purpose;
        this.channel = channel;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Purpose getPurpose() {
        return purpose;
    }

    public Channel getChannel() {
        return channel;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isConsumed() {
        return consumed;
    }

    public void setConsumed(boolean consumed) {
        this.consumed = consumed;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public enum Purpose {
        REGISTRATION,
        LOGIN_2FA
    }

    public enum Channel {
        EMAIL,
        SMS
    }
}
