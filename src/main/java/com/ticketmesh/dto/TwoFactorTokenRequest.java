package com.ticketmesh.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TwoFactorTokenRequest {

    @NotBlank
    @Size(max = 50)
    private String username;

    @NotBlank
    @Size(min = 4, max = 8)
    private String code;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}