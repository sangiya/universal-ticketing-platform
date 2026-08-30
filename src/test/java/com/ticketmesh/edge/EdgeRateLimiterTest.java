package com.ticketmesh.edge;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EdgeRateLimiterTest {

    private MutableClock clock;
    private EdgeRateLimiter limiter;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        limiter = new EdgeRateLimiter(clock);
    }

    @Test
    void allowsUpToLimitThenBlocks() {
        RateLimitRule rule = new RateLimitRule("test", 3, 60);
        assertTrue(limiter.tryAcquire("client:1", rule));
        assertTrue(limiter.tryAcquire("client:1", rule));
        assertTrue(limiter.tryAcquire("client:1", rule));
        assertFalse(limiter.tryAcquire("client:1", rule));
    }

    @Test
    void windowResetRestoresAllowance() {
        RateLimitRule rule = new RateLimitRule("test", 3, 60);
        for (int i = 0; i < 3; i++) {
            assertTrue(limiter.tryAcquire("client:1", rule));
        }
        assertFalse(limiter.tryAcquire("client:1", rule));

        clock.advanceSeconds(60);

        assertTrue(limiter.tryAcquire("client:1", rule));
    }

    @Test
    void keysAreIsolated() {
        RateLimitRule rule = new RateLimitRule("test", 1, 60);
        assertTrue(limiter.tryAcquire("client:1", rule));
        assertFalse(limiter.tryAcquire("client:1", rule));
        assertTrue(limiter.tryAcquire("client:2", rule));
    }

    @Test
    void statusReportsRemainingAndResetWindow() {
        RateLimitRule rule = new RateLimitRule("test", 5, 60);
        assertTrue(limiter.tryAcquire("client:1", rule));
        assertTrue(limiter.tryAcquire("client:1", rule));

        EdgeRateLimiter.RateLimitStatus status = limiter.status("client:1", rule);

        assertTrue(status.allowed());
        assertEquals(3, status.remaining());
        long epochSecond = clock.instant().getEpochSecond();
        long expectedReset = (epochSecond / 60 + 1) * 60;
        assertEquals(expectedReset, status.resetAtSeconds());
    }

    @Test
    void statusWhenExhaustedIsBlocked() {
        RateLimitRule rule = new RateLimitRule("test", 2, 60);
        assertTrue(limiter.tryAcquire("client:1", rule));
        assertTrue(limiter.tryAcquire("client:1", rule));

        EdgeRateLimiter.RateLimitStatus status = limiter.status("client:1", rule);

        assertFalse(status.allowed());
        assertEquals(0, status.remaining());
    }

    @Test
    void defaultTryAcquireUsesConfiguredRule() {
        assertTrue(limiter.tryAcquire("api.customer"));
        assertTrue(limiter.rules().stream()
                .anyMatch(rule -> rule.scope().equals("client") && rule.limit() == 60));
    }

    private static final class MutableClock extends Clock {
        private Instant instant;
        private final ZoneId zone = ZoneId.of("UTC");

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advanceSeconds(long seconds) {
            this.instant = this.instant.plusSeconds(seconds);
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}