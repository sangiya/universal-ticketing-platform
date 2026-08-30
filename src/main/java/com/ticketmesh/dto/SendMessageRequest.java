package com.ticketmesh.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SendMessageRequest {

    @NotBlank
    @Size(max = 20)
    private String channel;

    @NotBlank
    @Size(max = 120)
    private String recipientRef;

    @NotBlank
    @Size(max = 2000)
    private String body;

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
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