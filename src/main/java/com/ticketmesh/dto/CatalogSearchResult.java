package com.ticketmesh.dto;

import java.math.BigDecimal;

public record CatalogSearchResult(
        Long id,
        String title,
        String providerName,
        BigDecimal price,
        String currency) {
}
