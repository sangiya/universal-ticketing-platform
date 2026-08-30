package com.ticketmesh.dto;

import java.time.Instant;

public record ShopResponse(
        Long id,
        String shopName,
        String businessType,
        String countryIso,
        String currencyIso,
        String about,
        String contactEmail,
        String contactPhone,
        String status,
        Long tenantId,
        Instant appliedAt,
        Instant reviewedAt) {
}
