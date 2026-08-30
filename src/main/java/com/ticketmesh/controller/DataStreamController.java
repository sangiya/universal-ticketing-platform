package com.ticketmesh.controller;

import com.ticketmesh.dataplatform.EventStreamService;
import com.ticketmesh.dataplatform.IngestEvent;
import com.ticketmesh.dto.IngestAck;
import com.ticketmesh.dto.IngestEventRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Analytics ingest surface (admin/ops concept; the mapped route is secured by
 * default auth). Analytics consumers push domain events here and the read
 * side drains them deterministically.
 */
@RestController
@RequestMapping("/api/platform/ingest")
public class DataStreamController {

    private final EventStreamService eventStreamService;

    public DataStreamController(EventStreamService eventStreamService) {
        this.eventStreamService = eventStreamService;
    }

    @PostMapping
    public ResponseEntity<IngestAck> ingest(@Valid @RequestBody IngestEventRequest request) {
        String eventType = request.eventType().trim().toUpperCase();
        String payload = request.payload() == null ? "" : request.payload();
        eventStreamService.record(eventType, payload);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new IngestAck(eventType, "ACCEPTED"));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<IngestEvent>> pending(
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        return ResponseEntity.ok(eventStreamService.pending(limit));
    }
}