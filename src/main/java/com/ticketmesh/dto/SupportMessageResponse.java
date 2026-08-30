package com.ticketmesh.dto;

import java.time.Instant;

public record SupportMessageResponse(
        Long id,
        Long ticketId,
        String authorUsername,
        String body,
        Instant createdAt) {
}
