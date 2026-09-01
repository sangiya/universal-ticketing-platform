package com.ticketmesh.dto;

import com.ticketmesh.model.ProviderProduct.ProductType;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request body for the universal search console (spec §13). All fields are
 * optional; an empty request returns the latest live offers sorted by price.
 *
 * <p>The query supports text, location, date, passengers and filter facets. The
 * platform normalizes provider-specific responses into a single list of
 * {@link UniversalOffer}.
 */
public record UniversalSearchRequest(
        String q,
        String origin,
        String destination,
        LocalDate dateFrom,
        LocalDate dateTo,
        Integer passengers,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        ProductType productType,
        Boolean refundableOnly,
        String sortBy,    // "price", "date", "duration", "popularity"
        String sortDir,   // "asc", "desc"
        Integer limit
) {
    public UniversalSearchRequest {
        if (sortBy == null || sortBy.isBlank()) sortBy = "price";
        if (sortDir == null || sortDir.isBlank()) sortDir = "asc";
        if (limit == null || limit <= 0) limit = 50;
        if (limit > 200) limit = 200;
    }
}
