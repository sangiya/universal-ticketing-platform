package com.ticketmesh.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class WebhookRequest {

    @Size(max = 120)
    private String externalRef;

    @Size(max = 120)
    private String senderRef;

    @Size(max = 120)
    private String recipientRef;

    @NotBlank
    @Size(max = 2000)
    private String body;

    public String getExternalRef() {
        return externalRef;
    }

    public void setExternalRef(String externalRef) {
        this.externalRef = externalRef;
    }

    public String getSenderRef() {
        return senderRef;
    }

    public void setSenderRef(String senderRef) {
        this.senderRef = senderRef;
    }

    public String getRecipientRef() {
        return recipientRef;
    }

    public void setRecipientRef(String recipientRef) {
        this.recipientRef = recipientRef;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}