package com.ticketmesh.model;

import java.math.BigDecimal;

/**
 * The settled result of a cancellation, handed to
 * {@link ProductOrder#applyCancellation(RefundOutcome)} so the entity owns its own
 * status transition.
 */
public record RefundOutcome(
        boolean refunded,
        BigDecimal refundAmount,
        BigDecimal fee,
        String reason,
        String reference) {
}
