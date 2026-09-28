package com.ticketmesh.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record MarketplaceTicketResponse(
        String orderRef,
        String productTitle,
        String providerName,
        String productType,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal totalAmount,
        String currencyIso,
        String status,
        Instant createdAt,
        Instant paidAt,
        Instant holdExpiresAt,
        String qrData,
        /** Refund windows in force for this order, for the cancellation policy card. */
        List<CancellationPolicyResponse> cancellationPolicy) {
}
