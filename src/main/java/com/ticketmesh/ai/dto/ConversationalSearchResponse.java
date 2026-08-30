package com.ticketmesh.ai.dto;

import com.ticketmesh.ai.service.Intent;

import java.util.List;

public record ConversationalSearchResponse(Intent intent, List<SearchMatch> matches) {
}