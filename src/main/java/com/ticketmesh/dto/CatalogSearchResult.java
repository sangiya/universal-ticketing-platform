package com.ticketmesh.dto;

import java.math.BigDecimal;

public record CatalogSearchResult(
        Long id,
        String title,
        String providerName,
        String providerCode,
        BigDecimal price,
        String currency,
        String logoUrl,
        String themeColor) {
}
