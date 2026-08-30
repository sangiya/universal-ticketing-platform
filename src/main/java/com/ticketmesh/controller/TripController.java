package com.ticketmesh.controller;

import com.ticketmesh.dto.AddTripLegRequest;
import com.ticketmesh.dto.CreateTripRequest;
import com.ticketmesh.model.Trip;
import com.ticketmesh.model.TripItem;
import com.ticketmesh.service.TripService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Multi-service trip planner: bundle bookings across domains into one itinerary
 * and manage its legs.
 */
@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final TripService tripService;
    private final RequestContext requestContext;

    public TripController(TripService tripService, RequestContext requestContext) {
        this.tripService = tripService;
        this.requestContext = requestContext;
    }

    @PostMapping
    public ResponseEntity<Trip> create(@RequestBody CreateTripRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tripService.create(
                requestContext.currentTenantId(), requestContext.currentUserId(),
                request.title()));
    }

    @PostMapping("/{tripId}/legs")
    public ResponseEntity<Trip> addLeg(
            @PathVariable("tripId") Long tripId,
            @RequestBody AddTripLegRequest request) {
        return ResponseEntity.ok(tripService.addLeg(
                requestContext.currentTenantId(), requestContext.currentUserId(),
                tripId, request.bookingId(), request.note()));
    }

    @GetMapping
    public ResponseEntity<List<Trip>> listMine() {
        return ResponseEntity.ok(tripService.listMine(requestContext.currentUserId()));
    }

    @GetMapping("/{tripId}/legs")
    public ResponseEntity<List<TripItem>> legs(@PathVariable("tripId") Long tripId) {
        return ResponseEntity.ok(tripService.legs(tripId));
    }
}