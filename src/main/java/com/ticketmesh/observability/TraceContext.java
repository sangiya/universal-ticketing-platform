package com.ticketmesh.observability;

/**
 * A lightweight tracing span. Records the trace/span identity and the
 * operation name; the trace service hands these to callers so every request
 * can be correlated end-to-end. The same shape is exported to OTel/Jaeger at
 * the gateway, but the core keeps a zero-dependency surface.
 *
 * @param traceId   trace identity shared across the call chain
 * @param spanId    identity of this individual span
 * @param operation operation executed under this span
 */
public record TraceContext(
        String traceId,
        String spanId,
        String operation) {
}