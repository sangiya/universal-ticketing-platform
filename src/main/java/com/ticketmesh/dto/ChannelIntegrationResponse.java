package com.ticketmesh.dto;

import java.time.Instant;

public record ChannelIntegrationResponse(
        Long id,
        Long tenantId,
        String channel,
        String name,
        String apiKeyRef,
        boolean enabled,
        Instant createdAt) {
}