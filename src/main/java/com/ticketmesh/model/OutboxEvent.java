package com.ticketmesh.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * Outbox / event-driven message. Emitted when a domain event occurs and
 * delivered to async consumers, giving an event-driven architecture without a
 * heavyweight broker requirement in dev.
 */
@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 64)
    @Column(name = "event_id", nullable = false, unique = true, length = 64)
    private String eventId;

    @NotBlank
    @Size(max = 60)
    @Column(name = "aggregate_type", nullable = false, length = 60)
    private String aggregateType;

    @NotBlank
    @Size(max = 60)
    @Column(name = "aggregate_id", nullable = false, length = 60)
    private String aggregateId;

    @NotBlank
    @Size(max = 80)
    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;

    @NotBlank
    @Column(nullable = false, columnDefinition = "CLOB")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    public OutboxEvent() {
    }

    public OutboxEvent(String eventId, String aggregateType, String aggregateId,
                       String eventType, String payload) {
        this.eventId = eventId;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.status = Status.PENDING;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public Status getStatus() {
        return status;
    }

    public void markDelivered() {
        this.status = Status.DELIVERED;
        this.deliveredAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public enum Status {
        PENDING,
        DELIVERED,
        FAILED
    }
}
