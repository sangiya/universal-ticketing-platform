package com.ticketmesh.ml;

import java.math.BigDecimal;

public record DomainStat(
        String productType,
        long orderCount,
        BigDecimal revenue) {
}