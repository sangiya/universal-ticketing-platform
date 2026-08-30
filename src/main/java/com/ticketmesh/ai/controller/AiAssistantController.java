package com.ticketmesh.ai.controller;

import com.ticketmesh.ai.dto.AiAssistantRequest;
import com.ticketmesh.ai.dto.AiAssistantResponse;
import com.ticketmesh.ai.dto.ConversationalSearchResponse;
import com.ticketmesh.ai.dto.SearchMatch;
import com.ticketmesh.ai.service.AiAssistantService;
import com.ticketmesh.ai.service.Intent;
import com.ticketmesh.ai.service.IntentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
public class AiAssistantController {

    private final AiAssistantService assistantService;
    private final IntentService intentService;

    public AiAssistantController(AiAssistantService assistantService,
                                 IntentService intentService) {
        this.assistantService = assistantService;
        this.intentService = intentService;
    }

    @PostMapping("/assistant")
    public ResponseEntity<AiAssistantResponse> ask(@Valid @RequestBody AiAssistantRequest request) {
        return ResponseEntity.ok(assistantService.answer(request.getQuestion()));
    }

    @GetMapping("/search")
    public ResponseEntity<ConversationalSearchResponse> search(
            @RequestParam(name = "q") String query) {
        Intent intent = intentService.detectIntent(query);
        List<SearchMatch> matches = intentService.searchEntity(query);
        return ResponseEntity.ok(new ConversationalSearchResponse(intent, matches));
    }
}