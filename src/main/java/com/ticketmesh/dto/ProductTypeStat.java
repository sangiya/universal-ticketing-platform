package com.ticketmesh.dto;

import java.math.BigDecimal;

/** Universal per-product-type analytics row (not travel-specific). */
public record ProductTypeStat(
        String productType,
        String vertical,
        long productCount,
        long orderCount,
        BigDecimal revenue) {
}
