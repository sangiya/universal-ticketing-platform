package com.ticketmesh.dto;

import java.util.List;

import com.ticketmesh.edge.RateLimitRule;

/**
 * Edge probe health: status plus the active gateway rate-limit rules, so
 * load balancers and edge tooling can confirm the gateway surface is live
 * and discover its configured throttling in one call.
 */
public record EdgeHealthResponse(
        String status,
        List<RateLimitRule> rateLimits) {
}