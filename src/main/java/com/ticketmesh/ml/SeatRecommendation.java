package com.ticketmesh.ml;

import java.util.List;

public record SeatRecommendation(List<Integer> seats, double comfortScore, String reason) {
}