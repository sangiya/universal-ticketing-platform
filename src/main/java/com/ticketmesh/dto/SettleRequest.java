package com.ticketmesh.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class SettleRequest {

    @NotBlank
    @Pattern(regexp = "SUCCESS|FAILED", message = "outcome must be SUCCESS or FAILED")
    private String outcome;

    @Size(max = 40)
    private String reference;

    public String getOutcome() {
        return outcome;
    }

    public String getReference() {
        return reference;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }
}
