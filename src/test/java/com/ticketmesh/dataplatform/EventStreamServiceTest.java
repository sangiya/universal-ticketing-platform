package com.ticketmesh.dataplatform;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventStreamServiceTest {

    private IngestEventRepository ingestEventRepository;
    private EventStreamService eventStreamService;

    @BeforeEach
    void setUp() {
        ingestEventRepository = mock(IngestEventRepository.class);
        eventStreamService = new EventStreamService(ingestEventRepository);
        when(ingestEventRepository.save(any(IngestEvent.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void record_persistsPendingEvent() {
        eventStreamService.record("BOOKING_CREATED", "{\"bookingId\":7}");

        ArgumentCaptor<IngestEvent> captor = ArgumentCaptor.forClass(IngestEvent.class);
        verify(ingestEventRepository).save(captor.capture());
        assertEquals("BOOKING_CREATED", captor.getValue().getEventType());
        assertEquals("{\"bookingId\":7}", captor.getValue().getPayload());
        assertEquals(IngestEvent.ProcessingStatus.PENDING,
                captor.getValue().getProcessingStatus());
        assertNotNull(captor.getValue().getOccurredAt());
    }

    @Test
    void pending_limitsResultSet() {
        List<IngestEvent> all = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            all.add(new IngestEvent("PAYMENT_RECEIVED", "{}"));
        }
        when(ingestEventRepository.findByProcessingStatusOrderByIdAsc(
                IngestEvent.ProcessingStatus.PENDING)).thenReturn(all);

        assertEquals(3, eventStreamService.pending(3).size());
        assertEquals(5, eventStreamService.pending(0).size());
    }

    @Test
    void markProcessed_flipsStatus() {
        IngestEvent event = new IngestEvent("REFUND_CREATED", "{}");
        when(ingestEventRepository.findById(99L)).thenReturn(Optional.of(event));

        eventStreamService.markProcessed(99L);

        assertEquals(IngestEvent.ProcessingStatus.PROCESSED, event.getProcessingStatus());
        verify(ingestEventRepository).save(event);
    }

    @Test
    void markProcessed_ignoresUnknownId() {
        when(ingestEventRepository.findById(123L)).thenReturn(Optional.empty());

        eventStreamService.markProcessed(123L);

        verify(ingestEventRepository, org.mockito.Mockito.never())
                .save(any(IngestEvent.class));
    }

    @Test
    void countByType_delegatesToRepository() {
        when(ingestEventRepository.countByEventType("BOOKING_CREATED")).thenReturn(42L);

        assertEquals(42L, eventStreamService.countByType("BOOKING_CREATED"));
    }
}