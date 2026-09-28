package com.ticketmesh.service;

import com.ticketmesh.dto.RefundQuote;
import com.ticketmesh.model.RefundPolicyWindow;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * Pure pricing logic for cancellations (spec section 26).
 *
 * <p>Deliberately free of persistence and Spring wiring so the money maths can be
 * tested directly. The rules are:
 *
 * <ol>
 *   <li>A product with no policy at all is not refundable.</li>
 *   <li>The applicable window is the one with the largest
 *       {@code minHoursBeforeEvent} that is still satisfied, i.e. the most
 *       generous tier the customer qualifies for.</li>
 *   <li>Cancellation after the event has started is never refundable.</li>
 *   <li>The fee is the greater of a flat amount and a percentage of the amount
 *       paid, so a policy can express "min(10%, $25)".</li>
 *   <li>The refund is floored at zero and never exceeds what was paid.</li>
 * </ol>
 */
public final class RefundCalculator {

    private RefundCalculator() {
    }

    /**
     * Price a cancellation.
     *
     * @param orderRef        order being cancelled, echoed back for the caller's convenience
     * @param paidAmount      total actually charged to the customer
     * @param currencyIso     currency of {@code paidAmount}
     * @param eventStart      when the event begins; {@code null} for undated products
     * @param now             evaluation instant
     * @param windows         configured policy windows, may be empty
     */
    public static RefundQuote quote(String orderRef,
                                    BigDecimal paidAmount,
                                    String currencyIso,
                                    Instant eventStart,
                                    Instant now,
                                    List<RefundPolicyWindow> windows) {
        BigDecimal paid = paidAmount == null ? BigDecimal.ZERO : paidAmount;

        if (windows == null || windows.isEmpty()) {
            return new RefundQuote(orderRef, false, paid, BigDecimal.ZERO, paid,
                    BigDecimal.ZERO.setScale(2), "No cancellation policy configured", null, currencyIso);
        }

        if (eventStart == null) {
            return new RefundQuote(orderRef, false, paid, BigDecimal.ZERO, paid,
                    BigDecimal.ZERO.setScale(2), "This product has no scheduled event date", null, currencyIso);
        }

        if (!now.isBefore(eventStart)) {
            return new RefundQuote(orderRef, false, paid, BigDecimal.ZERO, paid,
                    BigDecimal.ZERO.setScale(2), "Event has already started", null, currencyIso);
        }

        long hoursOut = Duration.between(now, eventStart).toHours();

        RefundPolicyWindow window = windows.stream()
                .filter(w -> hoursOut >= w.getMinHoursBeforeEvent())
                .max(Comparator.comparingInt(RefundPolicyWindow::getMinHoursBeforeEvent))
                .orElse(null);

        if (window == null) {
            return new RefundQuote(orderRef, false, paid, BigDecimal.ZERO, paid,
                    BigDecimal.ZERO.setScale(2), "Outside all refund windows", null, currencyIso);
        }

        BigDecimal percent = window.getRefundPercent().max(BigDecimal.ZERO).min(new BigDecimal("100"));
        BigDecimal grossRefund = paid.multiply(percent)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        BigDecimal fee = feeFor(window, paid, grossRefund);
        BigDecimal refund = grossRefund.subtract(fee).max(BigDecimal.ZERO);
        refund = refund.min(paid).setScale(2, RoundingMode.HALF_UP);
        fee = paid.subtract(refund).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        String reason = refund.signum() > 0
                ? "Eligible for " + percent.stripTrailingZeros().toPlainString() + "% refund"
                : "Cancellation fee exceeds refund entitlement";

        return new RefundQuote(orderRef, refund.signum() > 0, paid, refund, fee,
                percent.setScale(2, RoundingMode.HALF_UP), reason, window.getLabel(), currencyIso);
    }

    /**
     * The customer pays the larger of the flat fee and the percentage fee, capped
     * so it can never exceed the gross refund.
     */
    private static BigDecimal feeFor(RefundPolicyWindow window, BigDecimal paid, BigDecimal grossRefund) {
        BigDecimal fee = BigDecimal.ZERO;
        if (window.getFeeAmount() != null && window.getFeeAmount().signum() > 0) {
            fee = window.getFeeAmount();
        }
        if (window.getFeePercent() != null && window.getFeePercent().signum() > 0) {
            BigDecimal percentFee = paid.multiply(window.getFeePercent())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            fee = fee.max(percentFee);
        }
        return fee.min(grossRefund).max(BigDecimal.ZERO);
    }
}
