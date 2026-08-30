package com.ticketmesh.dto;

public record TenantResponse(
        Long id,
        String slug,
        String name,
        String countryIso,
        String currencyIso,
        String defaultLanguage,
        String timezone,
        String domain,
        boolean enabled,
        int configVersion) {
}
