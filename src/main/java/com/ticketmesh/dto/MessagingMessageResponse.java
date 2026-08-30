package com.ticketmesh.dto;

import java.time.Instant;

public record MessagingMessageResponse(
        Long id,
        Long tenantId,
        String channel,
        String direction,
        String externalRef,
        String senderRef,
        String recipientRef,
        String body,
        String status,
        Instant createdAt) {
}