package com.ticketmesh.ml;

import java.math.BigDecimal;

public record PriceProjection(
        int dayOffset,
        String forecastDate,
        BigDecimal predictedPrice) {
}