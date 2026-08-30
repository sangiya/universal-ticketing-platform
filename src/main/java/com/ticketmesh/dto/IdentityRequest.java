package com.ticketmesh.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class IdentityRequest {

    @NotBlank
    @Size(max = 20)
    private String documentType;

    @NotBlank
    @Size(max = 64)
    private String documentNumber;

    @Size(max = 500)
    private String documentPhotoUrl;

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public String getDocumentPhotoUrl() {
        return documentPhotoUrl;
    }

    public void setDocumentPhotoUrl(String documentPhotoUrl) {
        this.documentPhotoUrl = documentPhotoUrl;
    }
}