package com.ticketmesh.dto;

import java.math.BigDecimal;

/**
 * Canonical, normalized offer returned by the universal search engine. Provider
 * responses are normalized into this model so upstream services (pricing,
 * ranking, booking) never depend on provider-specific schema.
 *
 * @param providerCode        business code of the source provider (e.g. "RAIL_LK")
 * @param offerId             unique id within the provider (or local product id)
 * @param title               human-readable title
 * @param origin              start location/segment
 * @param destination         end location/segment
 * @param price               total price in {@code currencyIso}
 * @param currencyIso         ISO-4217 currency
 * @param availableSeats      remaining capacity
 * @param vertical            business domain: BUS, TRAIN, MOVIE, EVENT, SPORTS, FLIGHT, FERRY, ATTRACTION
 * @param epochDepartureMillis departure time in UTC milliseconds
 * @param durationMinutes     total journey duration in minutes (0 if not applicable)
 * @param refundable          true if the offer is refundable per provider policy
 * @param rating              average customer rating 0..5 (0 if not rated)
 * @param attributes          optional JSON-encoded string of provider-specific attributes
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
        long epochDepartureMillis,
        int durationMinutes,
        boolean refundable,
        double rating,
        String attributes
) {
    public UniversalOffer(String providerCode, String offerId, String title,
                         String origin, String destination, BigDecimal price,
                         String currencyIso, int availableSeats, String vertical,
                         long epochDepartureMillis) {
        this(providerCode, offerId, title, origin, destination, price, currencyIso,
                availableSeats, vertical, epochDepartureMillis, 0, false, 0.0, null);
    }
}
