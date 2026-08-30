package com.ticketmesh.service;

import com.ticketmesh.model.OutboxEvent;
import com.ticketmesh.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Outbox / event-driven architecture. Domain events are queued reliably and
 * drained to async consumers. Keeps the platform event-driven without a broker
 * dependency in dev, while remaining broker-ready.
 */
@Service
public class EventService {

    private final OutboxEventRepository outboxEventRepository;

    public EventService(OutboxEventRepository outboxEventRepository) {
        this.outboxEventRepository = outboxEventRepository;
    }

    @Transactional
    public void emit(String aggregateType, String aggregateId, String eventType,
                     String payload) {
        OutboxEvent event = new OutboxEvent(
                UUID.randomUUID().toString(),
                aggregateType, aggregateId, eventType, payload);
        outboxEventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<OutboxEvent> pending(int limit) {
        List<OutboxEvent> events = outboxEventRepository.findByStatus(OutboxEvent.Status.PENDING);
        return limit > 0 && events.size() > limit ? events.subList(0, limit) : events;
    }

    @Transactional
    public void markDelivered(Long eventId) {
        outboxEventRepository.findById(eventId).ifPresent(e -> {
            e.markDelivered();
            outboxEventRepository.save(e);
        });
    }
}
