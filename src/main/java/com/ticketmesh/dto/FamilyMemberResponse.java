package com.ticketmesh.dto;

import java.time.Instant;

public record FamilyMemberResponse(Long userId, String username, String role,
                                   Instant joinedAt) {
}
