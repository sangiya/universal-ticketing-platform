package com.ticketmesh.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentStatusResponse(
        Long bookingId,
        String bookingRef,
        String bookingStatus,
        Long paymentId,
        String paymentStatus,
        BigDecimal amount,
        String currency,
        String method,
        String cardLast4,
        Instant createdAt,
        Instant paidAt) {
}
