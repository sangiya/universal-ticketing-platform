package com.ticketmesh.service;

import com.ticketmesh.dto.SupportTicketRequest;
import com.ticketmesh.dto.SupportTicketResponse;
import com.ticketmesh.model.SupportTicket;
import com.ticketmesh.model.Tenant;
import com.ticketmesh.model.User;
import com.ticketmesh.model.User.Role;
import com.ticketmesh.repository.SupportMessageRepository;
import com.ticketmesh.repository.SupportTicketRepository;
import com.ticketmesh.repository.UserRepository;
import com.ticketmesh.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SupportServiceTest {

    private SupportTicketRepository ticketRepository;
    private SupportMessageRepository messageRepository;
    private UserRepository userRepository;
    private TenantService tenantService;
    private CurrentUser currentUser;
    private SupportService supportService;

    private Tenant tenant;
    private User requester;
    private List<SupportTicket> saved;

    @BeforeEach
    void setUp() {
        ticketRepository = mock(SupportTicketRepository.class);
        messageRepository = mock(SupportMessageRepository.class);
        userRepository = mock(UserRepository.class);
        tenantService = mock(TenantService.class);
        currentUser = mock(CurrentUser.class);

        supportService = new SupportService(ticketRepository, messageRepository,
                userRepository, tenantService, currentUser);

        tenant = new Tenant("global", "Global", "LK", "LKR", "si", "Asia/Colombo", null);
        requester = new User("alice", "enc", "Alice", "alice@example.com", Role.CUSTOMER);

        saved = new ArrayList<>();
        when(ticketRepository.save(any(SupportTicket.class))).thenAnswer(inv -> {
            SupportTicket t = inv.getArgument(0);
            saved.add(t);
            return t;
        });

        when(tenantService.requireTenant("global")).thenReturn(tenant);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(requester));
        when(currentUser.username()).thenReturn("alice");
    }

    @Test
    void open_createsTicketWithRequestRefAndMediumPriorityByDefault() {
        SupportTicketRequest request = new SupportTicketRequest();
        request.setSubject("Payment refund");
        request.setCategory("REFUND");
        request.setDescription("Charge not reversed");

        SupportTicketResponse response = supportService.open("global", request);

        assertEquals("OPEN", response.status());
        assertEquals("REFUND", response.category());
        assertEquals("MEDIUM", response.priority());
        assertTrue(response.requestRef().startsWith("SUP-"));
        assertTrue(response.slaDueAt().isAfter(Instant.now()));
        assertEquals("alice", response.requesterUsername());
    }

    @Test
    void open_criticalPriorityGetsShorterSla() {
        SupportTicketRequest request = new SupportTicketRequest();
        request.setSubject("Site down");
        request.setCategory("OUTAGE");
        request.setPriority("CRITICAL");
        request.setDescription("Everything is failing");

        SupportTicketResponse response = supportService.open("global", request);

        assertEquals("CRITICAL", response.priority());
        long windowMillis = 60L * 60 * 1000 + 5000;
        long gap = response.slaDueAt().toEpochMilli() - Instant.now().toEpochMilli();
        assertTrue(gap <= windowMillis && gap > 0);
    }

    @Test
    void assign_setsAssigneeAndMovesToInProgress() {
        SupportTicket ticket = new SupportTicket(requester, tenant, "issue", "OTHER",
                SupportTicket.Priority.LOW, null, Instant.now().plusSeconds(3600));
        User agent = new User("support1", "enc", "Support", "s@x.com", Role.AGENT);

        when(ticketRepository.findById(7L)).thenReturn(Optional.of(ticket));
        when(userRepository.findByUsername("support1")).thenReturn(Optional.of(agent));

        SupportTicketResponse response = supportService.assign(7L, "support1");

        assertEquals("support1", response.assigneeUsername());
        assertEquals("IN_PROGRESS", response.status());
    }

    @Test
    void escalate_setsEscalatedFlagAndStatus() {
        SupportTicket ticket = new SupportTicket(requester, tenant, "issue", "OTHER",
                SupportTicket.Priority.HIGH, null, Instant.now().plusSeconds(3600));
        when(ticketRepository.findById(9L)).thenReturn(Optional.of(ticket));

        SupportTicketResponse response = supportService.escalate(9L);

        assertTrue(response.escalated());
        assertEquals("ESCALATED", response.status());
    }

    @Test
    void myTickets_returnsOnlyRequestersTickets() {
        SupportTicket ticket = new SupportTicket(requester, tenant, "a", "OTHER",
                SupportTicket.Priority.LOW, null, Instant.now().plusSeconds(3600));
        when(ticketRepository.findByRequester_IdOrderByCreatedAtDesc(requester.getId()))
                .thenReturn(List.of(ticket));

        List<SupportTicketResponse> tickets = supportService.myTickets();

        assertEquals(1, tickets.size());
        assertEquals("alice", tickets.get(0).requesterUsername());
    }

    @Test
    void autoEscalateSlaBreaches_flagsOverdueOpenTickets() {
        SupportTicket overdue = new SupportTicket(requester, tenant, "late", "OTHER",
                SupportTicket.Priority.MEDIUM, null, Instant.now().minusSeconds(60));
        when(ticketRepository.findBySlaDueAtBeforeAndStatusNotIn(any(Instant.class), any(List.class)))
                .thenReturn(List.of(overdue));

        supportService.autoEscalateSlaBreaches();

        assertTrue(overdue.isEscalated());
        assertEquals(SupportTicket.Status.ESCALATED, overdue.getStatus());
        verify(ticketRepository).save(overdue);
    }

    @Test
    void autoEscalateSlaBreaches_skipsAlreadyEscalated() {
        SupportTicket overdue = new SupportTicket(requester, tenant, "late", "OTHER",
                SupportTicket.Priority.MEDIUM, null, Instant.now().minusSeconds(60));
        overdue.setEscalated(true);
        when(ticketRepository.findBySlaDueAtBeforeAndStatusNotIn(any(Instant.class), any(List.class)))
                .thenReturn(List.of(overdue));

        supportService.autoEscalateSlaBreaches();

        assertTrue(overdue.isEscalated());
        assertEquals(SupportTicket.Status.OPEN, overdue.getStatus());
    }
}
