package com.ticketmesh.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * A special deal / offer on a product. These are distinct from regular tickets
 * because they have a promotional discount or time-limited pricing.
 *
 * @param id               local product id
 * @param providerCode     provider's business code
 * @param providerName     display name of the provider
 * @param productType      ROUTE | ADMISSION | SEAT | PACKAGE | SERVICE
 * @param title            offer title
 * @param origin           departure city / origin
 * @param destination      arrival city / destination (null for non-route)
 * @param eventDate        scheduled departure / event time
 * @param currentPrice     price WITH the deal applied
 * @param originalPrice    price BEFORE the deal (for showing discount)
 * @param discountPercent  % off (0 if flat discount)
 * @param dealType         FLAT_OFF | PERCENTAGE_OFF | BUY_X_GET_Y | FLASH_SALE
 * @param dealTag          short label shown on the badge e.g. "🔥 20% OFF"
 * @param validUntil       when the deal expires
 * @param seatsLeft        remaining discounted seats
 * @param totalSeats       total seats in the deal
 * @param currencyIso      currency code
 * @param thumbnailUrl     optional image
 */
public record OfferResponse(
        Long id,
        String providerCode,
        String providerName,
        String productType,
        String title,
        String origin,
        String destination,
        LocalDateTime eventDate,
        BigDecimal currentPrice,
        BigDecimal originalPrice,
        int discountPercent,
        DealType dealType,
        String dealTag,
        Instant validUntil,
        int seatsLeft,
        int totalSeats,
        String currencyIso,
        String thumbnailUrl
) {
    public enum DealType {
        FLAT_OFF,
        PERCENTAGE_OFF,
        BUY_X_GET_Y,
        FLASH_SALE
    }
}
