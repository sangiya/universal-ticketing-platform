package com.ticketmesh.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AppKeyVerifyRequest {

    @NotBlank
    @Size(max = 50)
    private String username;

    @NotBlank
    @Size(min = 16, max = 128)
    private String key;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}