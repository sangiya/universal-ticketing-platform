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
 * A localized UI string for a tenant/locale. Supports any language anywhere by
 * configuration (no code): the platform is multi-language/global ready.
 */
@Entity
@Table(name = "i18n_messages")
public class I18nMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id")
    private Long tenantId;

    @NotBlank
    @Size(max = 10)
    @Column(nullable = false, length = 10)
    private String locale;

    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String messageKey;

    @NotBlank
    @Size(max = 1000)
    @Column(nullable = false, length = 1000)
    private String messageValue;

    @Column(nullable = false)
    private Instant updatedAt;

    public I18nMessage() {
    }

    public I18nMessage(Long tenantId, String locale, String messageKey, String messageValue) {
        this.tenantId = tenantId;
        this.locale = locale;
        this.messageKey = messageKey;
        this.messageValue = messageValue;
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public String getLocale() {
        return locale;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public String getMessageValue() {
        return messageValue;
    }

    public void setMessageValue(String messageValue) {
        this.messageValue = messageValue;
        this.updatedAt = Instant.now();
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
