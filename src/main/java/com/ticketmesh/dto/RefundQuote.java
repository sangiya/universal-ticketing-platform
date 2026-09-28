package com.ticketmesh.dto;

import java.math.BigDecimal;

/**
 * Outcome of pricing a cancellation against a product's refund policy
 * (spec section 26). Returned before any state is mutated so the customer can
 * see the exact amount they will get back.
 */
public record RefundQuote(
        String orderRef,
        boolean refundable,
        BigDecimal paidAmount,
        BigDecimal refundAmount,
        BigDecimal cancellationFee,
        BigDecimal refundPercent,
        String reason,
        String windowLabel,
        String currencyIso) {
}
