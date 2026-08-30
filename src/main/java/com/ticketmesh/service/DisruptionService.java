package com.ticketmesh.service;

import com.ticketmesh.dto.DisruptionReport;
import com.ticketmesh.model.Booking;
import com.ticketmesh.model.OutboxEvent;
import com.ticketmesh.repository.BookingRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Disruption & recovery intelligence. Assesses operational health by combining
 * recent cancellations (customer-facing signals) with the outbox backlog
 * (integration-delivery signals) and emits actionable recovery steps.
 */
@Service
public class DisruptionService {

    private static final Duration WINDOW = Duration.ofHours(24);
    private static final int PENDING_EVENT_LIMIT = 50;

    private final EventService eventService;
    private final BookingRepository bookingRepository;

    public DisruptionService(EventService eventService, BookingRepository bookingRepository) {
        this.eventService = eventService;
        this.bookingRepository = bookingRepository;
    }

    public DisruptionReport assess(Long tenantId) {
        Instant now = Instant.now();
        Instant cutoff = now.minus(WINDOW);

        List<Booking> disrupted = bookingRepository
                .findByStatusAndCreatedAtBefore(Booking.Status.CANCELLED, now)
                .stream()
                .filter(b -> b.getCreatedAt() != null && b.getCreatedAt().isAfter(cutoff))
                .toList();

        List<OutboxEvent> pending = eventService.pending(PENDING_EVENT_LIMIT);

        List<String> findings = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        if (!disrupted.isEmpty()) {
            findings.add(disrupted.size()
                    + " booking(s) were cancelled in the last 24 hours; customers are being "
                    + "impacted by a service disruption.");
            recommendations.add("Re-book or re-route affected customers on the next "
                    + "available departure or alternative provider.");
            recommendations.add("Proactively notify impacted passengers and coordinate "
                    + "recovery with the responsible providers.");
        }
        if (!pending.isEmpty()) {
            findings.add(pending.size()
                    + " outbox event(s) are still pending delivery; the asynchronous "
                    + "integration pipeline is backlogged.");
            recommendations.add("Monitor and drain the outbox so event delivery "
                    + "to downstream consumers resumes.");
        }

        return new DisruptionReport(disrupted.size(), findings, recommendations);
    }
}