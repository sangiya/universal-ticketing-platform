package com.ticketmesh.dto;

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
        long totalBookings) {
}
