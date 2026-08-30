package com.ticketmesh.dataplatform;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * One streaming ingest row on the analytics read-model surface. Events are
 * queued PENDING, drained by consumers and marked PROCESSED so the drain is
 * deterministic and replay-safe.
 */
@Entity
@Table(name = "ingest_events")
public class IngestEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 60)
    @Column(name = "event_type", nullable = false, length = 60)
    private String eventType;

    @Size(max = 2000)
    @Column(length = 2000)
    private String payload;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 20)
    private ProcessingStatus processingStatus;

    public IngestEvent() {
    }

    public IngestEvent(String eventType, String payload) {
        this.eventType = eventType;
        this.payload = payload;
        this.processingStatus = ProcessingStatus.PENDING;
        this.occurredAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public ProcessingStatus getProcessingStatus() {
        return processingStatus;
    }

    public void markProcessed() {
        this.processingStatus = ProcessingStatus.PROCESSED;
    }

    public enum ProcessingStatus {
        PENDING,
        PROCESSED,
        FAILED
    }
}