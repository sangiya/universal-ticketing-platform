package com.ticketmesh.dto;

import java.math.BigDecimal;
import java.util.List;

public record TripPlan(String origin, String destination, int legCount, List<TripLeg> legs,
                       BigDecimal totalFare, boolean feasible, String feasibilityNote) {
}