package com.ticketmesh.dto;

public class UserSettingsRequest {

    private String theme;
    private String language;
    private String currency;
    private Boolean notifyEmail;
    private Boolean notifySms;
    private Boolean notifyPush;
    private Boolean notifyWhatsapp;

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

    public Boolean getNotifyEmail() {
        return notifyEmail;
    }

    public void setNotifyEmail(Boolean notifyEmail) {
        this.notifyEmail = notifyEmail;
    }

    public Boolean getNotifySms() {
        return notifySms;
    }

    public void setNotifySms(Boolean notifySms) {
        this.notifySms = notifySms;
    }

    public Boolean getNotifyPush() {
        return notifyPush;
    }

    public void setNotifyPush(Boolean notifyPush) {
        this.notifyPush = notifyPush;
    }

    public Boolean getNotifyWhatsapp() {
        return notifyWhatsapp;
    }

    public void setNotifyWhatsapp(Boolean notifyWhatsapp) {
        this.notifyWhatsapp = notifyWhatsapp;
    }
}
