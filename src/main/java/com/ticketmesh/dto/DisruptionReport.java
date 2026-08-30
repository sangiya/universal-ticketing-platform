package com.ticketmesh.dto;

import java.util.List;

/**
 * Disruption & recovery intelligence snapshot. Counts active disruptions and
 * gives deterministic findings plus actionable recovery recommendations.
 */
public record DisruptionReport(int activeDisruptions, List<String> findings,
                               List<String> recoveryRecommendations) {
}