package com.ticketmesh.edge;

/**
 * A configurable rate-limit rule applied at the edge/gateway layer.
 *
 * @param scope         rule scope, e.g. api.global / tenant / client / ip
 * @param limit         max requests allowed within a window
 * @param windowSeconds length of the fixed window in seconds
 */
public record RateLimitRule(
        String scope,
        int limit,
        int windowSeconds) {
}