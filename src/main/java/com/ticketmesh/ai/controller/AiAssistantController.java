package com.ticketmesh.ai.controller;

import com.ticketmesh.ai.dto.AiAssistantRequest;
import com.ticketmesh.ai.dto.AiAssistantResponse;
import com.ticketmesh.ai.dto.ConversationalSearchResponse;
import com.ticketmesh.ai.dto.SearchMatch;
import com.ticketmesh.ai.service.AiAssistantService;
import com.ticketmesh.ai.service.Intent;
import com.ticketmesh.ai.service.IntentService;
import com.ticketmesh.dto.UniversalOffer;
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

    /**
     * Conversational search: parse natural-language query into structured
     * intent and return matching offers. Echoes the parsed fields back so the
     * client can show chips (From, To, Date, Passengers, Domain).
     */
    @GetMapping("/search")
    public ResponseEntity<ConversationalSearchResponse> search(
            @RequestParam(name = "q") String query,
            @RequestParam(name = "tenantId", required = false) Long tenantId) {
        Intent intent = intentService.detectIntent(query);
        List<SearchMatch> matches = intentService.searchEntity(query);
        // Always include parsed-intent metadata so the client can render chips
        return ResponseEntity.ok(new ConversationalSearchResponse(intent, matches,
                intentService.parse(query), null));
    }

    /**
     * Conversational search that returns universal offers directly.
     * Falls back to the deterministic offline path when no LLM is available.
     */
    @GetMapping("/search/offers")
    public ResponseEntity<List<UniversalOffer>> searchOffers(
            @RequestParam(name = "q") String query,
            @RequestParam(name = "tenantId", required = false) Long tenantId,
            @RequestParam(name = "limit", required = false, defaultValue = "10") int limit) {
        return ResponseEntity.ok(intentService.searchWithParsedIntent(query, tenantId, limit));
    }
}
