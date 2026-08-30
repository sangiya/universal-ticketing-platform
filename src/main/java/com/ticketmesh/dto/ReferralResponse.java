package com.ticketmesh.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import java.time.Instant;

public record ReferralResponse(Long id, String code, String status, String inviteeEmailMasked,
                               Long inviteeUserId, long rewardPoints, Instant createdAt,
                               Instant joinedAt) {
}
