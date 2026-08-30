package com.ticketmesh.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Per-user preference / notification settings. Created lazily with the
 * platform defaults the first time a user opens their settings screen.
 */
@Entity
@Table(name = "user_settings")
public class UserSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @NotBlank
    @Column(nullable = false, length = 20)
    private String theme = "LIGHT";

    @NotBlank
    @Column(nullable = false, length = 8)
    private String language = "en";

    @NotBlank
    @Column(nullable = false, length = 3)
    private String currency = "LKR";

    @Column(name = "notify_email", nullable = false)
    private boolean notifyEmail = true;

    @Column(name = "notify_sms", nullable = false)
    private boolean notifySms = false;

    @Column(name = "notify_push", nullable = false)
    private boolean notifyPush = true;

    @Column(name = "notify_whatsapp", nullable = false)
    private boolean notifyWhatsapp = false;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UserSettings() {
    }

    public UserSettings(Long userId) {
        this.userId = userId;
        this.theme = "LIGHT";
        this.language = "en";
        this.currency = "LKR";
        this.notifyEmail = true;
        this.notifySms = false;
        this.notifyPush = true;
        this.notifyWhatsapp = false;
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public boolean isNotifyEmail() {
        return notifyEmail;
    }

    public void setNotifyEmail(boolean notifyEmail) {
        this.notifyEmail = notifyEmail;
    }

    public boolean isNotifySms() {
        return notifySms;
    }

    public void setNotifySms(boolean notifySms) {
        this.notifySms = notifySms;
    }

    public boolean isNotifyPush() {
        return notifyPush;
    }

    public void setNotifyPush(boolean notifyPush) {
        this.notifyPush = notifyPush;
    }

    public boolean isNotifyWhatsapp() {
        return notifyWhatsapp;
    }

    public void setNotifyWhatsapp(boolean notifyWhatsapp) {
        this.notifyWhatsapp = notifyWhatsapp;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void markUpdated() {
        this.updatedAt = Instant.now();
    }
}
