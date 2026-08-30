package com.ticketmesh.dto;

import java.time.Instant;

public record FamilyGroupResponse(Long id, String name, Long ownerUserId,
                                  long memberCount, Instant createdAt) {
}
