package com.ticketmesh.ai.dto;

import java.util.List;

public class AiAssistantResponse {

    private final String answer;
    private final String guardrail;
    private final List<String> sources;

    public AiAssistantResponse(String answer, String guardrail, List<String> sources) {
        this.answer = answer;
        this.guardrail = guardrail;
        this.sources = sources;
    }

    public String getAnswer() {
        return answer;
    }

    public String getGuardrail() {
        return guardrail;
    }

    public List<String> getSources() {
        return sources;
    }
}
