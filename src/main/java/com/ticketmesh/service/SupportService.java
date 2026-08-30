package com.ticketmesh.service;

import com.ticketmesh.dto.SupportMessageRequest;
import com.ticketmesh.dto.SupportMessageResponse;
import com.ticketmesh.dto.SupportTicketRequest;
import com.ticketmesh.dto.SupportTicketResponse;
import com.ticketmesh.exception.ConflictException;
import com.ticketmesh.exception.NotFoundException;
import com.ticketmesh.model.SupportMessage;
import com.ticketmesh.model.SupportTicket;
import com.ticketmesh.model.SupportTicket.Priority;
import com.ticketmesh.model.SupportTicket.Status;
import com.ticketmesh.model.Tenant;
import com.ticketmesh.model.User;
import com.ticketmesh.repository.SupportMessageRepository;
import com.ticketmesh.repository.SupportTicketRepository;
import com.ticketmesh.repository.UserRepository;
import com.ticketmesh.security.CurrentUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 24/7 support service: open tickets, reply, assign, escalate, and manage the
 * support queue with SLA deadlines. An automated escalation task keeps an eye on
 * SLA breaches and happy-path resolutions, powering the "support portal" used by
 * customers and the operations/support team.
 */
@Service
public class SupportService {

    private static final Logger log = LoggerFactory.getLogger(SupportService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SupportTicketRepository ticketRepository;
    private final SupportMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final TenantService tenantService;
    private final CurrentUser currentUser;

    public SupportService(SupportTicketRepository ticketRepository,
                          SupportMessageRepository messageRepository,
                          UserRepository userRepository,
                          TenantService tenantService,
                          CurrentUser currentUser) {
        this.ticketRepository = ticketRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.tenantService = tenantService;
        this.currentUser = currentUser;
    }

    @Transactional
    public SupportTicketResponse open(String tenantSlug, SupportTicketRequest request) {
        Tenant tenant = tenantService.requireTenant(tenantSlug);
        User requester = currentUser();
        Priority priority = parsePriority(request.getPriority());
        Duration sla = slaFor(priority);
        SupportTicket ticket = new SupportTicket(
                requester, tenant, request.getSubject(), request.getCategory(),
                priority, request.getDescription(), Instant.now().plus(sla));
        ticket.setRequestRef(generateRef());
        ticketRepository.save(ticket);
        log.info("Opened support ticket {} for {} ({})",
                ticket.getRequestRef(), requester.getUsername(), priority);
        return toResponse(ticket);
    }

    @Transactional(readOnly = true)
    public List<SupportTicketResponse> myTickets() {
        User requester = currentUser();
        return ticketRepository.findByRequester_IdOrderByCreatedAtDesc(requester.getId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<SupportTicketResponse> allForTenant(String tenantSlug) {
        Tenant tenant = tenantService.requireTenant(tenantSlug);
        return ticketRepository.findByTenant_IdOrderByCreatedAtDesc(tenant.getId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<SupportTicketResponse> queue(Status status) {
        return ticketRepository.findByStatusOrderByCreatedAtDesc(status)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<SupportTicketResponse> escalated() {
        return ticketRepository.findByEscalatedTrueOrderByCreatedAtDesc()
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public SupportTicketResponse assign(Long ticketId, String assigneeUsername) {
        SupportTicket ticket = requireTicket(ticketId);
        User assignee = userRepository.findByUsername(assigneeUsername)
                .orElseThrow(() -> new NotFoundException("User not found: " + assigneeUsername));
        ticket.setAssignee(assignee);
        if (ticket.getStatus() == Status.OPEN) {
            ticket.setStatus(Status.IN_PROGRESS);
        }
        ticketRepository.save(ticket);
        return toResponse(ticket);
    }

    @Transactional
    public SupportTicketResponse updateStatus(Long ticketId, Status status) {
        SupportTicket ticket = requireTicket(ticketId);
        if (status != Status.CLOSED && status != Status.RESOLVED
                && status != Status.WAITING_CUSTOMER) {
            throw new ConflictException(
                    "Only CLOSED, RESOLVED or WAITING_CUSTOMER can be set directly");
        }
        ticket.setStatus(status);
        ticketRepository.save(ticket);
        return toResponse(ticket);
    }

    @Transactional
    public SupportTicketResponse escalate(Long ticketId) {
        SupportTicket ticket = requireTicket(ticketId);
        ticket.setEscalated(true);
        ticket.setStatus(Status.ESCALATED);
        ticketRepository.save(ticket);
        return toResponse(ticket);
    }

    @Transactional
    public SupportMessageResponse reply(Long ticketId, SupportMessageRequest request) {
        SupportTicket ticket = requireTicket(ticketId);
        User author = currentUser();
        SupportMessage message = new SupportMessage(ticket, author, request.getBody());
        messageRepository.save(message);
        ticket.touch();
        return new SupportMessageResponse(
                message.getId(), ticket.getId(), author.getUsername(),
                message.getBody(), message.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<SupportMessageResponse> conversation(Long ticketId) {
        requireTicket(ticketId);
        return messageRepository.findByTicket_IdOrderByCreatedAtAsc(ticketId)
                .stream()
                .map(m -> new SupportMessageResponse(
                        m.getId(), m.getTicket().getId(), m.getAuthor().getUsername(),
                        m.getBody(), m.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public long openCount() {
        return ticketRepository.countByStatus(Status.OPEN)
                + ticketRepository.countByStatus(Status.IN_PROGRESS);
    }

    /**
     * Automated SLA sweep: any ticket past its SLA due time that is not resolved
     * or closed is escalated automatically so nobody misses the 24/7 support
     * commitment. Runs on a fixed delay.
     */
    @Scheduled(fixedDelayString = "${app.support.sla-check-ms:60000}")
    @Transactional
    public void autoEscalateSlaBreaches() {
        List<SupportTicket.Status> done = List.of(Status.RESOLVED, Status.CLOSED);
        List<SupportTicket> overdue = ticketRepository
                .findBySlaDueAtBeforeAndStatusNotIn(Instant.now(), done);
        for (SupportTicket ticket : overdue) {
            if (!ticket.isEscalated()) {
                ticket.setEscalated(true);
                ticket.setStatus(Status.ESCALATED);
                ticketRepository.save(ticket);
                log.warn("Auto-escalated SLA breach on ticket {}", ticket.getRequestRef());
            }
        }
    }

    private Duration slaFor(Priority priority) {
        return switch (priority) {
            case CRITICAL -> Duration.ofHours(1);
            case HIGH -> Duration.ofHours(4);
            case MEDIUM -> Duration.ofHours(24);
            case LOW -> Duration.ofHours(72);
        };
    }

    private Priority parsePriority(String raw) {
        if (raw == null || raw.isBlank()) {
            return Priority.MEDIUM;
        }
        try {
            return Priority.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ConflictException("Unsupported priority: " + raw);
        }
    }

    private String generateRef() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder("SUP-");
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private SupportTicket requireTicket(Long ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new NotFoundException("Support ticket not found: " + ticketId));
    }

    private User currentUser() {
        return userRepository.findByUsername(currentUser.username())
                .orElseThrow(() -> new NotFoundException("Authenticated user not found"));
    }

    private SupportTicketResponse toResponse(SupportTicket t) {
        return new SupportTicketResponse(
                t.getId(), t.getRequestRef(), t.getTenant().getId(),
                t.getRequester().getUsername(),
                t.getAssignee() == null ? null : t.getAssignee().getUsername(),
                t.getSubject(), t.getCategory(), t.getPriority().name(),
                t.getStatus().name(), t.getDescription(), t.getSlaDueAt(),
                t.isEscalated(), t.getCreatedAt(), t.getResolvedAt());
    }
}
