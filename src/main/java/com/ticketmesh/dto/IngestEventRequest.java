package com.ticketmesh.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for the ingest surface: an analytics consumer pushes a domain event
 * with a JSON-string payload for the read model.
 */
public record IngestEventRequest(
        @NotBlank
        @Size(max = 60)
        String eventType,
        @Size(max = 2000)
        String payload) {
}