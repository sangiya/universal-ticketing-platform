package com.ticketmesh.ml;

import java.math.BigDecimal;

public record DynamicPriceProjection(BigDecimal basePrice, double surgeRate,
                                     BigDecimal projectedPrice, boolean guardrailActive) {
}