package com.ticketmesh.dto;

/**
 * Acknowledgment returned once an ingest event is queued for the read model.
 */
public record IngestAck(
        String eventType,
        String status) {
}