package com.ticketmesh.dto;

import java.util.Set;

public record FraudCheckResponse(
        int score,
        String risk,
        Set<String> flags,
        boolean blocked) {
}
