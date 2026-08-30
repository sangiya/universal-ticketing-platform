package com.ticketmesh.dataplatform;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Streaming ingest / read-model surface. Domain events are persisted as
 * PENDING rows, drained deterministically by analytics consumers in id order
 * and marked PROCESSED once handled - a broker-free stand-in for Kafka
 * consumption that stays queue-safe across restarts.
 */
@Component
public class EventStreamService {

    private final IngestEventRepository ingestEventRepository;

    public EventStreamService(IngestEventRepository ingestEventRepository) {
        this.ingestEventRepository = ingestEventRepository;
    }

    @Transactional
    public void record(String eventType, String payload) {
        ingestEventRepository.save(new IngestEvent(eventType, payload));
    }

    @Transactional(readOnly = true)
    public List<IngestEvent> pending(int limit) {
        List<IngestEvent> events = ingestEventRepository
                .findByProcessingStatusOrderByIdAsc(IngestEvent.ProcessingStatus.PENDING);
        return limit > 0 && events.size() > limit ? events.subList(0, limit) : events;
    }

    @Transactional
    public void markProcessed(Long id) {
        ingestEventRepository.findById(id).ifPresent(event -> {
            event.markProcessed();
            ingestEventRepository.save(event);
        });
    }

    @Transactional(readOnly = true)
    public long countByType(String eventType) {
        return ingestEventRepository.countByEventType(eventType);
    }
}