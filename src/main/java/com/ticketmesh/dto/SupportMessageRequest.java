package com.ticketmesh.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SupportMessageRequest {

    @NotBlank
    @Size(max = 2000)
    private String body;

    public String getBody() {
        return body;
    }
}
