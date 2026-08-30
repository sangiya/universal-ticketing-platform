package com.ticketmesh.ml;

import java.util.Map;

public record AnalystAnswer(
        String question,
        String answer,
        Map<String, Object> data) {
}