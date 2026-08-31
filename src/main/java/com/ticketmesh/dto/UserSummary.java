package com.ticketmesh.dto;

import java.time.Instant;

public record UserSummary(
        Long id,
        String username,
        String fullName,
        String email,
        String role,
        String status,
        Long tenantId,
        Instant createdAt) {
}
