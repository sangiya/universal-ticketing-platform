package com.ticketmesh.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        Long paymentId,
        String paymentRef,
        String bookingRef,
        BigDecimal amount,
        String currency,
        String method,
        String status,
        String cardLast4,
        Instant paidAt) {
}
