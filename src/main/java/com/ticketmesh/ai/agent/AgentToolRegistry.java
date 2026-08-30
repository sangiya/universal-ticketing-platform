package com.ticketmesh.ai.agent;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Curated registry of the AI agent's invocable tools. Every tool declares the
 * explicit permission its invocation requires, so no tool can ever be called
 * without an authorization decision being made against the caller's role.
 */
@Component
public class AgentToolRegistry {

    private static final List<AgentTool> TOOLS = List.of(
            new AgentTool("catalog.search", "catalog:read",
                    "Search the enabled catalog for tickets, services and products."),
            new AgentTool("route.lookup", "catalog:read",
                    "Look up rail routes and schedules between two stations."),
            new AgentTool("pricing.check", "pricing:read",
                    "Check current price, fare breakdown and surge projections."),
            new AgentTool("support.lookup", "support:read",
                    "Retrieve support answers, refund policies and FAQ entries."),
            new AgentTool("booking.status", "booking:read",
                    "Check the status of a booking or reservation."),
            new AgentTool("admin.report", "admin:read",
                    "Generate tenant-wide analytics and revenue reports."));

    public List<AgentTool> allTools() {
        return TOOLS;
    }
}