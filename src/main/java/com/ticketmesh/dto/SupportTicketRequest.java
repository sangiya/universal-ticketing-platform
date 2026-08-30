package com.ticketmesh.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SupportTicketRequest {

    @NotBlank
    @Size(max = 200)
    private String subject;

    @NotBlank
    @Size(max = 40)
    private String category;

    private String priority;

    @Size(max = 2000)
    private String description;

    public String getSubject() {
        return subject;
    }

    public String getCategory() {
        return category;
    }

    public String getPriority() {
        return priority;
    }

    public String getDescription() {
        return description;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
