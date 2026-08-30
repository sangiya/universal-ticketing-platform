package com.ticketmesh.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ChannelConfigRequest {

    @NotBlank
    @Size(max = 20)
    private String channel;

    @NotBlank
    @Size(max = 120)
    private String name;

    @Size(max = 120)
    private String apiKeyRef;

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
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
}