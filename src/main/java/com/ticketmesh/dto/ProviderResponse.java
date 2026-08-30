package com.ticketmesh.dto;

import java.time.Instant;

public record ProviderResponse(
        Long id,
        String code,
        String name,
        Long shopId,
        Long tenantId,
        String countryIso,
        String currencyIso,
        String timezone,
        String apiEndpoint,
        String authMode,
        String vertical,
        String capabilities,
        String status,
        Instant createdAt) {
}
