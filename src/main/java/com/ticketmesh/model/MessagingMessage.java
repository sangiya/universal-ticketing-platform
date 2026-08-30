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
 * A messaging conversation entry over an external channel (WhatsApp / Facebook /
 * Telegram / SMS). Inbound entries are created by the channel webhook; outbound
 * entries are sent through the messaging adapter. This is the omnichannel
 * surface that lets customers buy and chat via their preferred social app.
 */
@Entity
@Table(name = "messaging_messages")
public class MessagingMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @NotBlank
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Channel channel;

    @NotBlank
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Direction direction;

    @Size(max = 120)
    @Column(name = "external_ref", length = 120)
    private String externalRef;

    @Size(max = 120)
    @Column(name = "sender_ref", length = 120)
    private String senderRef;

    @Size(max = 120)
    @Column(name = "recipient_ref", length = 120)
    private String recipientRef;

    @NotBlank
    @Size(max = 2000)
    @Column(nullable = false, length = 2000)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public MessagingMessage() {
    }

    public MessagingMessage(Tenant tenant, Channel channel, Direction direction, String externalRef,
                            String senderRef, String recipientRef, String body) {
        this.tenant = tenant;
        this.channel = channel;
        this.direction = direction;
        this.externalRef = externalRef;
        this.senderRef = senderRef;
        this.recipientRef = recipientRef;
        this.body = body;
        this.status = Status.RECEIVED;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public Channel getChannel() {
        return channel;
    }

    public Direction getDirection() {
        return direction;
    }

    public String getExternalRef() {
        return externalRef;
    }

    public String getSenderRef() {
        return senderRef;
    }

    public String getRecipientRef() {
        return recipientRef;
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
        WHATSAPP,
        FACEBOOK,
        TELEGRAM,
        SMS
    }

    public enum Direction {
        INBOUND,
        OUTBOUND
    }

    public enum Status {
        RECEIVED,
        PROCESSED,
        SENT,
        FAILED
    }
}
