package com.ticketmesh.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * A notification message for a recipient (in-app / email / push). Supports the
 * all-e-services requirement (booking confirmations, alerts, support updates).
 */
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id")
    private Long tenantId;

    @NotNull
    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Channel channel;

    @Size(max = 200)
    @Column(length = 200)
    private String subject;

    @NotBlank
    @Size(max = 2000)
    @Column(nullable = false, length = 2000)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public Notification() {
    }

    public Notification(Long tenantId, Long recipientId, Channel channel, String subject,
                        String body) {
        this.tenantId = tenantId;
        this.recipientId = recipientId;
        this.channel = channel;
        this.subject = subject;
        this.body = body;
        this.status = Status.PENDING;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public Long getRecipientId() {
        return recipientId;
    }

    public Channel getChannel() {
        return channel;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
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

    public enum Channel {
        IN_APP,
        EMAIL,
        PUSH,
        SMS,
        WHATSAPP,
        FACEBOOK,
        TELEGRAM
    }

    public enum Status {
        PENDING,
        SENT,
        FAILED
    }
}
