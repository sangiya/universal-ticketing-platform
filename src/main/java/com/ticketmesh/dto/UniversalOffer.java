package com.ticketmesh.dto;

import java.math.BigDecimal;

/**
 * Canonical, normalized offer returned by any provider adapter. Provider
 * responses are normalized into this model so upstream services (search,
 * pricing, ranking) never depend on provider-specific schema. This matches the
 * domain rule "no provider-specific schema in core domain".
 */
public record UniversalOffer(
        String providerCode,
        String offerId,
        String title,
        String origin,
        String destination,
        BigDecimal price,
        String currencyIso,
        int availableSeats,
        String vertical,
        long epochDepartureMillis) {
}
