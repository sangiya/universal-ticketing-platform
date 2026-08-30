package com.ticketmesh.dto;

import java.time.Instant;

public record SupportTicketResponse(
        Long id,
        String requestRef,
        Long tenantId,
        String requesterUsername,
        String assigneeUsername,
        String subject,
        String category,
        String priority,
        String status,
        String description,
        Instant slaDueAt,
        boolean escalated,
        Instant createdAt,
        Instant resolvedAt) {
}
