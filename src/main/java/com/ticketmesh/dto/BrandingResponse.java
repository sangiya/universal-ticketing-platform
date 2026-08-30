package com.ticketmesh.dto;

import java.time.Instant;

public record BrandingResponse(
        Long tenantId,
        String slug,
        String brandName,
        String tagline,
        String primaryColor,
        String secondaryColor,
        String accentColor,
        String logoUrl,
        String bannerUrl,
        String appIconUrl,
        String fontFamily,
        int borderRadius,
        boolean darkMode,
        String homeHeroTitle,
        String homeHeroSubtitle,
        Instant updatedAt) {
}
