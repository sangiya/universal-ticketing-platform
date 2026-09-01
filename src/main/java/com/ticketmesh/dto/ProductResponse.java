package com.ticketmesh.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProductResponse(
        Long id,
        Long providerId,
        String providerName,
        String providerCode,
        Long tenantId,
        String productType,
        String title,
        String origin,
        String destination,
        LocalDateTime eventDate,
        BigDecimal price,
        String currencyIso,
        int availableQuantity,
        String description,
        String attributes,
        boolean enabled,
        String logoUrl,
        String themeColor,
        String tagline,
        Instant createdAt,
        // ── Movie / event metadata ──
        String language,
        String genre,
        String format,
        Integer durationMinutes,
        BigDecimal ratingStars,
        String castList,
        String director,
        LocalDate releaseDate,
        String posterUrl,
        String bannerUrl,
        boolean isPremiere,
        boolean isNowShowing) {
}
