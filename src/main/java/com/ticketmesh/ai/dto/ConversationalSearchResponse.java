package com.ticketmesh.ai.dto;

import com.ticketmesh.ai.service.Intent;
import com.ticketmesh.ai.service.IntentService;
import com.ticketmesh.dto.UniversalOffer;

import java.util.List;

public record ConversationalSearchResponse(
        Intent intent,
        List<SearchMatch> matches,
        IntentService.ParsedIntent parsed,
        List<UniversalOffer> offers
) {
    public ConversationalSearchResponse(Intent intent, List<SearchMatch> matches) {
        this(intent, matches, null, null);
    }
}
