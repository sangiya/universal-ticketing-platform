package com.ticketmesh.service;

import com.ticketmesh.dto.RefundQuote;
import com.ticketmesh.model.RefundPolicyWindow;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RefundCalculatorTest {

    private static final Instant NOW = Instant.parse("2026-03-01T10:00:00Z");
    private static final BigDecimal PAID = new BigDecimal("5000.00");

    private static RefundPolicyWindow window(int minHours, String percent, String feeAmount, String feePercent) {
        return new RefundPolicyWindow(1L, minHours,
                new BigDecimal(percent),
                feeAmount == null ? null : new BigDecimal(feeAmount),
                feePercent == null ? null : new BigDecimal(feePercent),
                minHours + "h+");
    }

    @Test
    void noPolicy_isNotRefundable() {
        RefundQuote q = RefundCalculator.quote("TM-1", PAID, "LKR", NOW.plus(30, ChronoUnit.DAYS), NOW, List.of());

        assertFalse(q.refundable());
        assertEquals(0, q.refundAmount().compareTo(BigDecimal.ZERO));
        assertEquals(PAID, q.refundAmount().add(q.cancellationFee()));
        assertEquals("No cancellation policy configured", q.reason());
    }

    @Test
    void noEventDate_isNotRefundable() {
        RefundQuote q = RefundCalculator.quote("TM-1", PAID, "LKR", null, NOW,
                List.of(window(72, "100", null, null)));

        assertFalse(q.refundable());
        assertEquals("This product has no scheduled event date", q.reason());
    }

    @Test
    void afterEventStart_isNotRefundable() {
        RefundQuote q = RefundCalculator.quote("TM-1", PAID, "LKR", NOW.minus(1, ChronoUnit.HOURS), NOW,
                List.of(window(0, "100", null, null)));

        assertFalse(q.refundable());
        assertEquals("Event has already started", q.reason());
    }

    @Test
    void outsideAllWindows_isNotRefundable() {
        // Only a 24h+ window exists but the event is 2 hours away.
        RefundQuote q = RefundCalculator.quote("TM-1", PAID, "LKR", NOW.plus(2, ChronoUnit.HOURS), NOW,
                List.of(window(24, "100", null, null)));

        assertFalse(q.refundable());
        assertEquals("Outside all refund windows", q.reason());
    }

    @Test
    void mostGenerousSatisfiedWindowWins() {
        RefundQuote q = RefundCalculator.quote("TM-1", PAID, "LKR", NOW.plus(200, ChronoUnit.HOURS), NOW,
                List.of(
                        window(24, "50", null, null),
                        window(168, "90", null, null),
                        window(72, "75", null, null)));

        assertTrue(q.refundable());
        assertEquals(0, q.refundPercent().compareTo(new BigDecimal("90")));
        assertEquals(0, q.refundAmount().compareTo(new BigDecimal("4500.00")));
        assertEquals("168h+", q.windowLabel());
    }

    @Test
    void fullRefund_hasNoFee() {
        RefundQuote q = RefundCalculator.quote("TM-1", PAID, "LKR", NOW.plus(200, ChronoUnit.HOURS), NOW,
                List.of(window(72, "100", null, null)));

        assertTrue(q.refundable());
        assertEquals(0, q.refundAmount().compareTo(PAID));
        assertEquals(0, q.cancellationFee().compareTo(BigDecimal.ZERO));
    }

    @Test
    void flatFee_isDeductedFromRefund() {
        RefundQuote q = RefundCalculator.quote("TM-1", PAID, "LKR", NOW.plus(48, ChronoUnit.HOURS), NOW,
                List.of(window(24, "100", "250.00", null)));

        assertTrue(q.refundable());
        assertEquals(0, q.refundAmount().compareTo(new BigDecimal("4750.00")));
        assertEquals(0, q.cancellationFee().compareTo(new BigDecimal("250.00")));
    }

    @Test
    void greaterOfFlatOrPercentFee_applies() {
        // 10% of 5000 = 500, which beats the flat 250.
        RefundQuote q = RefundCalculator.quote("TM-1", PAID, "LKR", NOW.plus(48, ChronoUnit.HOURS), NOW,
                List.of(window(24, "100", "250.00", "10")));

        assertEquals(0, q.refundAmount().compareTo(new BigDecimal("4500.00")));
        assertEquals(0, q.cancellationFee().compareTo(new BigDecimal("500.00")));
    }

    @Test
    void flatFeeWinsWhenLarger() {
        // 10% of 5000 = 500, but the flat 900 is larger.
        RefundQuote q = RefundCalculator.quote("TM-1", PAID, "LKR", NOW.plus(48, ChronoUnit.HOURS), NOW,
                List.of(window(24, "100", "900.00", "10")));

        assertEquals(0, q.cancellationFee().compareTo(new BigDecimal("900.00")));
    }

    @Test
    void feeLargerThanEntitlement_floorsRefundAtZero() {
        // Entitlement is 10% of 5000 = 500, but the flat fee of 900 wipes it out.
        RefundQuote q = RefundCalculator.quote("TM-1", PAID, "LKR", NOW.plus(48, ChronoUnit.HOURS), NOW,
                List.of(window(24, "10", "900.00", null)));

        assertFalse(q.refundable());
        assertEquals(0, q.refundAmount().compareTo(BigDecimal.ZERO));
        assertEquals(PAID, q.cancellationFee());
    }

    @Test
    void feeNeverExceedsEntitlement() {
        // 50% refund = 2500, but the flat fee is 4000.
        RefundQuote q = RefundCalculator.quote("TM-1", PAID, "LKR", NOW.plus(48, ChronoUnit.HOURS), NOW,
                List.of(window(24, "50", "4000.00", null)));

        assertFalse(q.refundable());
        assertEquals(0, q.refundAmount().compareTo(BigDecimal.ZERO));
        assertEquals(PAID, q.cancellationFee());
    }

    @Test
    void refundPlusFeeAlwaysEqualsPaid() {
        RefundQuote q = RefundCalculator.quote("TM-1", PAID, "LKR", NOW.plus(10, ChronoUnit.HOURS), NOW,
                List.of(window(0, "25", "100.00", "5")));

        assertEquals(0, q.refundAmount().add(q.cancellationFee()).compareTo(PAID));
    }

    @Test
    void zeroPercentWindow_isNotRefundable() {
        RefundQuote q = RefundCalculator.quote("TM-1", PAID, "LKR", NOW.plus(10, ChronoUnit.HOURS), NOW,
                List.of(window(0, "0", null, null)));

        assertFalse(q.refundable());
        assertEquals("Cancellation fee exceeds refund entitlement", q.reason());
    }

    @Test
    void boundaryExactlyAtWindow_qualifies() {
        // Event exactly 24h away should satisfy a 24h window.
        RefundQuote q = RefundCalculator.quote("TM-1", PAID, "LKR", NOW.plus(24, ChronoUnit.HOURS), NOW,
                List.of(window(24, "100", null, null)));

        assertTrue(q.refundable());
        assertEquals(0, q.refundAmount().compareTo(PAID));
    }

    @Test
    void unsortedWindows_stillPicksMostGenerous() {
        RefundQuote q = RefundCalculator.quote("TM-1", PAID, "LKR", NOW.plus(200, ChronoUnit.HOURS), NOW,
                List.of(
                        window(168, "90", null, null),
                        window(24, "50", null, null),
                        window(72, "75", null, null)));

        assertEquals(0, q.refundPercent().compareTo(new BigDecimal("90")));
    }

    @Test
    void overHundredPercent_isCapped() {
        RefundQuote q = RefundCalculator.quote("TM-1", PAID, "LKR", NOW.plus(200, ChronoUnit.HOURS), NOW,
                List.of(window(72, "150", null, null)));

        assertEquals(0, q.refundAmount().compareTo(PAID));
        assertEquals(0, q.refundPercent().compareTo(new BigDecimal("100")));
    }
}
