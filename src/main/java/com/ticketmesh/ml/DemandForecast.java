package com.ticketmesh.ml;

import java.util.List;

public record DemandForecast(
        Long productId,
        int horizonDays,
        double confidence,
        List<DayProjection> projections) {
}