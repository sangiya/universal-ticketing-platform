package com.ticketmesh.ml;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Deterministic dynamic-pricing model. A surge multiplier is derived from
 * remaining capacity scarcity, live demand and the base load factor, then
 * clamped by the override guardrail so a projected price can never exceed
 * {@link #MAX_MULTIPLIER} times the base price.
 */
@Component
public class DynamicPricingEngine {

    public static final double MAX_MULTIPLIER = 1.6;

    private record SurgeResult(double rate, boolean clamped) {
    }

    public double surgeRate(int demandScore, int capacityRemaining, int capacityTotal,
                            double baseLoadFactor) {
        return computeSurge(demandScore, capacityRemaining, capacityTotal, baseLoadFactor)
                .rate();
    }

    public DynamicPriceProjection projectedPrice(BigDecimal basePrice, int demandScore,
                                                 int capacityRemaining, int capacityTotal) {
        BigDecimal base = basePrice == null ? BigDecimal.ZERO : basePrice;
        SurgeResult surge = computeSurge(demandScore, capacityRemaining, capacityTotal, 0.5);
        BigDecimal projected = base.multiply(BigDecimal.valueOf(surge.rate()))
                .setScale(2, RoundingMode.HALF_UP);
        return new DynamicPriceProjection(base.setScale(2, RoundingMode.HALF_UP),
                surge.rate(), projected, surge.clamped());
    }

    private SurgeResult computeSurge(int demandScore, int capacityRemaining, int capacityTotal,
                                     double baseLoadFactor) {
        int demand = clampInt(demandScore, 0, 100);
        double load = clamp(baseLoadFactor, 0.0, 1.0);
        double availability = availability(capacityRemaining, capacityTotal);
        double scarcity = Math.max(0.0, 1.0 - availability);
        double scarcitySurge = scarcity * scarcity * 0.55;
        double demandRatio = demand / 100.0;
        double demandSurge = demandRatio * demandRatio * 0.30;
        double loadSurge = load * 0.08;
        double raw = 1.0 + scarcitySurge + demandSurge + loadSurge;
        boolean clamped = raw > MAX_MULTIPLIER;
        double rate = round4(clamped ? MAX_MULTIPLIER : Math.max(1.0, raw));
        return new SurgeResult(rate, clamped);
    }

    private double availability(int remaining, int total) {
        if (total <= 0) {
            return 0.0;
        }
        double ratio = remaining / (double) total;
        return clamp(ratio, 0.0, 1.0);
    }

    private int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private double round4(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }
}