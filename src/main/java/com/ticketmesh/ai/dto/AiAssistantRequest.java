package com.ticketmesh.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AiAssistantRequest {

    @NotBlank(message = "question must not be blank")
    @Size(max = 2000, message = "question must be at most 2000 characters")
    private String question;

    public AiAssistantRequest() {
    }

    public AiAssistantRequest(String question) {
        this.question = question;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }
}
