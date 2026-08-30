package com.ticketmesh.dto;

import java.time.Instant;

public record IdentityResponse(
        Long id,
        Long userId,
        String documentType,
        String documentNumberMasked,
        String documentPhotoUrl,
        String status,
        Long verifiedBy,
        Instant verifiedAt,
        Instant createdAt) {
}