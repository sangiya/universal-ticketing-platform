package com.ticketmesh.dto;

import java.math.BigDecimal;

/**
 * One refund window shown in the cancellation policy (spec section 26).
 *
 * @param minHoursBeforeEvent hours before the event at which this window starts applying
 * @param refundPercent       share of the paid amount returned inside the window
 * @param label               provider-supplied human label
 */
public record CancellationPolicyResponse(
        int minHoursBeforeEvent,
        BigDecimal refundPercent,
        BigDecimal feeAmount,
        BigDecimal feePercent,
        String label) {

    public boolean refundable() {
        return refundPercent != null && refundPercent.signum() > 0;
    }
}
