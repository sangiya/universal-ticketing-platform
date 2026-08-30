package com.ticketmesh.observability;

import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Lightweight tracing surface. Spans are generated deterministically from the
 * caller-supplied operations and trace ids so tests and tooling can rely on
 * stable identities. Export to OTel/Jaeger happens at the gateway; the core
 * stays dependency-free.
 */
@Component
public class TraceService {

    private static final String HEX_PAD = "0000000000000000";

    public TraceContext start(String operation) {
        return span(UUID.randomUUID().toString().replace("-", ""), operation);
    }

    public TraceContext child(TraceContext parent, String operation) {
        return span(parent.traceId(), operation);
    }

    public TraceContext span(String traceId, String operation) {
        return new TraceContext(traceId, nextSpanId(traceId), operation);
    }

    private String nextSpanId(String traceId) {
        String hex = Integer.toHexString(traceId.hashCode());
        if (hex.length() > 16) {
            return hex.substring(0, 16);
        }
        return hex + HEX_PAD.substring(hex.length());
    }
}