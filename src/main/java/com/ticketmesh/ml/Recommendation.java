package com.ticketmesh.ml;

public record Recommendation(
        Long productId,
        String title,
        String productType,
        double score,
        String reason) {
}