package com.ticketmesh.service;

import com.ticketmesh.dto.DisruptionReport;
import com.ticketmesh.model.Booking;
import com.ticketmesh.model.OutboxEvent;
import com.ticketmesh.model.TrainRoute;
import com.ticketmesh.model.TrainSchedule;
import com.ticketmesh.model.User;
import com.ticketmesh.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DisruptionServiceTest {

    private EventService eventService;
    private BookingRepository bookingRepository;
    private DisruptionService disruptionService;

    @BeforeEach
    void setUp() {
        eventService = mock(EventService.class);
        bookingRepository = mock(BookingRepository.class);
        disruptionService = new DisruptionService(eventService, bookingRepository);
        when(eventService.pending(anyInt())).thenReturn(List.of());
    }

    private Booking cancelledBooking() {
        User user = new User("alice", "encoded", "Alice", "alice@example.com",
                User.Role.CUSTOMER);
        TrainRoute route = new TrainRoute("COL-KAN", "Colombo-Kandy", "Colombo", "Kandy",
                new BigDecimal("1200.00"), 115);
        TrainSchedule schedule = new TrainSchedule(route, "EX-1001",
                LocalDate.now().plusDays(1), LocalTime.of(8, 30), LocalTime.of(12, 15),
                60, new BigDecimal("1200.00"));
        return new Booking("ticketmesh-DISR1", user, schedule, schedule.getServiceDate(),
                "Alice", 7, new BigDecimal("1200.00"), Booking.Status.CANCELLED);
    }

    @Test
    void assess_reportsDisruptionWhenRecentCancellationsExist() {
        when(bookingRepository.findByStatusAndCreatedAtBefore(
                eq(Booking.Status.CANCELLED), any())).thenReturn(List.of(cancelledBooking()));

        DisruptionReport report = disruptionService.assess(1L);

        assertEquals(1, report.activeDisruptions());
        assertTrue(report.findings().stream()
                .anyMatch(f -> f.contains("cancelled in the last 24 hours")));
        assertTrue(report.recoveryRecommendations().stream()
                .anyMatch(r -> r.contains("Re-book or re-route")));
    }

    @Test
    void assess_recommendsMonitoringWhenOutboxBacklogged() {
        when(bookingRepository.findByStatusAndCreatedAtBefore(
                eq(Booking.Status.CANCELLED), any())).thenReturn(List.of());
        when(eventService.pending(anyInt())).thenReturn(List.of(
                new OutboxEvent("evt-1", "booking", "b1", "Booking.Paid", "{}")));

        DisruptionReport report = disruptionService.assess(1L);

        assertEquals(0, report.activeDisruptions());
        assertTrue(report.findings().stream()
                .anyMatch(f -> f.contains("still pending delivery")));
        assertTrue(report.recoveryRecommendations().stream()
                .anyMatch(r -> r.contains("Monitor and drain the outbox")));
    }

    @Test
    void assess_reportsCleanWhenNothingDisrupted() {
        when(bookingRepository.findByStatusAndCreatedAtBefore(
                eq(Booking.Status.CANCELLED), any())).thenReturn(List.of());

        DisruptionReport report = disruptionService.assess(1L);

        assertEquals(0, report.activeDisruptions());
        assertTrue(report.findings().isEmpty());
        assertTrue(report.recoveryRecommendations().isEmpty());
    }
}