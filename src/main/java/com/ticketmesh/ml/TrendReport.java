package com.ticketmesh.ml;

import java.math.BigDecimal;
import java.util.List;

public record TrendReport(
        Long tenantId,
        long totalOrders,
        BigDecimal totalRevenue,
        BigDecimal averageOrderValue,
        List<DomainStat> domains) {
}