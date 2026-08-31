package com.ticketmesh.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderSummary(
        Long id,
        String orderRef,
        String providerName,
        String productTitle,
        String productType,
        int quantity,
        BigDecimal unitPrice,
        String currencyIso,
        BigDecimal totalAmount,
        String status,
        Instant createdAt) {
}
