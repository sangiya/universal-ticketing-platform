package com.ticketmesh.ml;

public record DayProjection(
        int dayOffset,
        double projectedDemand,
        double seasonalFactor,
        String forecastDate) {
}