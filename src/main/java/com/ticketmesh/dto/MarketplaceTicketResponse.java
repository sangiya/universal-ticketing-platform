package com.ticketmesh.dto;

import java.math.BigDecimal;
import java.time.Instant;

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
        String qrData) {
}
