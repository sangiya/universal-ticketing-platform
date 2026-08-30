package com.ticketmesh.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ProviderConnectRequest {

    @NotBlank
    @Size(max = 40)
    private String code;

    @NotBlank
    @Size(max = 160)
    private String name;

    @Size(max = 500)
    private String apiEndpoint;

    @NotBlank
    @Size(max = 20)
    private String authMode;

    @NotBlank
    private String vertical;

    private String capabilities;

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getApiEndpoint() {
        return apiEndpoint;
    }

    public String getAuthMode() {
        return authMode;
    }

    public String getVertical() {
        return vertical;
    }

    public String getCapabilities() {
        return capabilities;
    }
}
