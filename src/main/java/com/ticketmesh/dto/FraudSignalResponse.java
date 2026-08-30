package com.ticketmesh.dto;

import java.time.Instant;

public record FraudSignalResponse(
        Long id,
        Long tenantId,
        String subjectType,
        String subjectRef,
        String actorUsername,
        int score,
        String risk,
        String flags,
        String details,
        Instant createdAt) {
}
