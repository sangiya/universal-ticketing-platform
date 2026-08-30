package com.ticketmesh.ai.controller;

import com.ticketmesh.ai.dto.AiAssistantRequest;
import com.ticketmesh.ai.dto.AiAssistantResponse;
import com.ticketmesh.ai.service.AiAssistantService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiAssistantController {

    private final AiAssistantService assistantService;

    public AiAssistantController(AiAssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @PostMapping("/assistant")
    public ResponseEntity<AiAssistantResponse> ask(@Valid @RequestBody AiAssistantRequest request) {
        return ResponseEntity.ok(assistantService.answer(request.getQuestion()));
    }
}
