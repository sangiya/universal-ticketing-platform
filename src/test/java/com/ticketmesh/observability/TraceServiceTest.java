package com.ticketmesh.observability;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class TraceServiceTest {

    private final TraceService traceService = new TraceService();

    @Test
    void startGeneratesFullSpan() {
        TraceContext context = traceService.start("BOOKING");

        assertEquals("BOOKING", context.operation());
        assertEquals(32, context.traceId().length());
        assertEquals(16, context.spanId().length());
        assertNotNull(context.traceId());
        assertNotNull(context.spanId());
    }

    @Test
    void childInheritsTraceIdAndGetsNewSpan() {
        TraceContext root = new TraceContext("trace-root-42", "span-root", "BOOKING_START");

        TraceContext child = traceService.child(root, "BOOKING_CREATE");

        assertEquals("trace-root-42", child.traceId());
        assertEquals("BOOKING_CREATE", child.operation());
        assertNotEquals(root.spanId(), child.spanId());
    }

    @Test
    void spanIsDeterministicForSameInput() {
        TraceContext first = traceService.span("trace-42", "CHECKOUT");
        TraceContext second = traceService.span("trace-42", "CHECKOUT");

        assertEquals(first.traceId(), second.traceId());
        assertEquals(first.spanId(), second.spanId());
        assertEquals(first.operation(), second.operation());
    }

    @Test
    void spanIdDiffersAcrossTraceRoots() {
        TraceContext one = traceService.span("trace-A", "OP");
        TraceContext two = traceService.span("trace-B", "OP");

        assertNotEquals(one.spanId(), two.spanId());
    }
}