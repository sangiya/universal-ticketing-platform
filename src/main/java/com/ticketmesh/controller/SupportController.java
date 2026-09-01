package com.ticketmesh.controller;

import com.ticketmesh.dto.SupportMessageRequest;
import com.ticketmesh.dto.SupportMessageResponse;
import com.ticketmesh.dto.SupportTicketRequest;
import com.ticketmesh.dto.SupportTicketResponse;
import com.ticketmesh.model.SupportTicket.Status;
import com.ticketmesh.service.SupportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 24/7 support portal API. Customers open and follow their tickets; the support
 * team and admins manage the queue, assign, reply, escalate and close.
 */
@RestController
@RequestMapping("/api/support")
public class SupportController {

    private final SupportService supportService;
    private final RequestContext requestContext;
    private final com.ticketmesh.repository.TenantRepository tenantRepository;

    public SupportController(SupportService supportService,
                             RequestContext requestContext,
                             com.ticketmesh.repository.TenantRepository tenantRepository) {
        this.supportService = supportService;
        this.requestContext = requestContext;
        this.tenantRepository = tenantRepository;
    }

    private String currentUserSlug() {
        Long tenantId = requestContext.currentTenantId();
        if (tenantId == null) return "global";
        return tenantRepository.findById(tenantId)
                .map(com.ticketmesh.model.Tenant::getSlug)
                .orElse("global");
    }

    @PostMapping("/tickets")
    public ResponseEntity<SupportTicketResponse> open(
            @RequestParam(value = "tenant", required = false) String tenantSlug,
            @Valid @RequestBody SupportTicketRequest request) {
        String resolvedTenant = tenantSlug != null
                ? tenantSlug
                : currentUserSlug();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(supportService.open(resolvedTenant, request));
    }

    @GetMapping("/tickets/me")
    public ResponseEntity<List<SupportTicketResponse>> myTickets() {
        return ResponseEntity.ok(supportService.myTickets());
    }

    @PostMapping("/tickets/{id}/messages")
    public ResponseEntity<SupportMessageResponse> reply(
            @PathVariable("id") Long ticketId,
            @Valid @RequestBody SupportMessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(supportService.reply(ticketId, request));
    }

    @GetMapping("/tickets/{id}/messages")
    public ResponseEntity<List<SupportMessageResponse>> conversation(
            @PathVariable("id") Long ticketId) {
        return ResponseEntity.ok(supportService.conversation(ticketId));
    }

    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    @GetMapping("/admin/tickets")
    public ResponseEntity<List<SupportTicketResponse>> allForTenant(
            @RequestParam("tenant") String tenantSlug) {
        return ResponseEntity.ok(supportService.allForTenant(tenantSlug));
    }

    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    @GetMapping("/admin/queue")
    public ResponseEntity<List<SupportTicketResponse>> queue(
            @RequestParam("status") Status status) {
        return ResponseEntity.ok(supportService.queue(status));
    }

    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    @GetMapping("/admin/escalated")
    public ResponseEntity<List<SupportTicketResponse>> escalated() {
        return ResponseEntity.ok(supportService.escalated());
    }

    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    @PutMapping("/admin/tickets/{id}/assign")
    public ResponseEntity<SupportTicketResponse> assign(
            @PathVariable("id") Long ticketId,
            @RequestParam("assignee") String assigneeUsername) {
        return ResponseEntity.ok(supportService.assign(ticketId, assigneeUsername));
    }

    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    @PutMapping("/admin/tickets/{id}/status")
    public ResponseEntity<SupportTicketResponse> updateStatus(
            @PathVariable("id") Long ticketId, @RequestParam("status") Status status) {
        return ResponseEntity.ok(supportService.updateStatus(ticketId, status));
    }

    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    @PutMapping("/admin/tickets/{id}/escalate")
    public ResponseEntity<SupportTicketResponse> escalate(@PathVariable("id") Long ticketId) {
        return ResponseEntity.ok(supportService.escalate(ticketId));
    }
}
