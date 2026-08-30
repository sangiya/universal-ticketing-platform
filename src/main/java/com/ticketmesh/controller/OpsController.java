package com.ticketmesh.controller;

import com.ticketmesh.dto.DisruptionReport;
import com.ticketmesh.model.OutboxEvent;
import com.ticketmesh.service.DisruptionService;
import com.ticketmesh.service.EventService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Operations endpoints: outbox backlog visibility and disruption &
 * recovery intelligence.
 */
@RestController
@RequestMapping("/api/ops")
public class OpsController {

    private final EventService eventService;
    private final DisruptionService disruptionService;
    private final RequestContext requestContext;

    public OpsController(EventService eventService, DisruptionService disruptionService,
                         RequestContext requestContext) {
        this.eventService = eventService;
        this.disruptionService = disruptionService;
        this.requestContext = requestContext;
    }

    @GetMapping("/events")
    public ResponseEntity<List<OutboxEvent>> pending(
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        return ResponseEntity.ok(eventService.pending(limit));
    }

    @GetMapping("/disruption")
    public ResponseEntity<DisruptionReport> disruption(
            @RequestParam(value = "tenantId", required = false) Long tenantId) {
        return ResponseEntity.ok(disruptionService.assess(requestContext.tenantIdOr(tenantId)));
    }
}