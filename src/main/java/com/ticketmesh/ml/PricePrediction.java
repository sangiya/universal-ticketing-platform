package com.ticketmesh.ml;

import java.math.BigDecimal;
import java.util.List;

public record PricePrediction(
        Long productId,
        BigDecimal basePrice,
        double growthRate,
        int horizonDays,
        List<PriceProjection> projections) {
}