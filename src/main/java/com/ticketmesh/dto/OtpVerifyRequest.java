package com.ticketmesh.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class OtpVerifyRequest {

    @NotBlank
    @Size(max = 160)
    private String email;

    @NotBlank
    @Size(min = 4, max = 8)
    private String code;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}