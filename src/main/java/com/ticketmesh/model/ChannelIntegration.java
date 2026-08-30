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
 * A per-tenant outbound messaging channel integration (WhatsApp / Facebook /
 * Telegram / SMS). API keys are stored by reference (env-seeded), never inline.
 */
@Entity
@Table(name = "channel_integrations")
public class ChannelIntegration {

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
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String name;

    @Size(max = 120)
    @Column(name = "api_key_ref", length = 120)
    private String apiKeyRef;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public ChannelIntegration() {
    }

    public ChannelIntegration(Tenant tenant, Channel channel, String name, String apiKeyRef) {
        this.tenant = tenant;
        this.channel = channel;
        this.name = name;
        this.apiKeyRef = apiKeyRef;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getApiKeyRef() {
        return apiKeyRef;
    }

    public void setApiKeyRef(String apiKeyRef) {
        this.apiKeyRef = apiKeyRef;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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
}
