package com.ticketmesh.dto;

import java.math.BigDecimal;
import java.util.List;

public record AdminStatsResponse(
        long totalUsers,
        long customers,
        long agents,
        long admins,
        long totalTenants,
        long activeTenants,
        long totalProviders,
        long activeProviders,
        long pendingShops,
        long approvedShops,
        long totalProducts,
        long enabledProducts,
        long totalBookings,
        // universal commerce metrics (all domains, not travel-only)
        long totalOrders,
        BigDecimal gmv,
        String gmvCurrency,
        long distinctProductTypes,
        List<ProductTypeStat> productTypeStats) {

    /** Backwards-compatible constructor for existing callers. */
    public AdminStatsResponse(long totalUsers, long customers, long agents, long admins,
                              long totalTenants, long activeTenants, long totalProviders,
                              long activeProviders, long pendingShops, long approvedShops,
                              long totalProducts, long enabledProducts, long totalBookings) {
        this(totalUsers, customers, agents, admins, totalTenants, activeTenants,
                totalProviders, activeProviders, pendingShops, approvedShops,
                totalProducts, enabledProducts, totalBookings, 0L,
                BigDecimal.ZERO, "LKR", 0, List.of());
    }
}
