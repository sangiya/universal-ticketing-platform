package com.ticketmesh.controller;

import com.ticketmesh.dto.TripPlan;
import com.ticketmesh.service.TripPlannerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trips")
public class TripPlannerController {

    private final TripPlannerService tripPlannerService;

    public TripPlannerController(TripPlannerService tripPlannerService) {
        this.tripPlannerService = tripPlannerService;
    }

    @GetMapping("/plan")
    public ResponseEntity<TripPlan> plan(
            @RequestParam(name = "tenantId", required = false) Long tenantId,
            @RequestParam(name = "origin") String origin,
            @RequestParam(name = "destination") String destination,
            @RequestParam(name = "legs", defaultValue = "2") int legs,
            @RequestParam(name = "startDate", required = false) String startDate) {
        return ResponseEntity.ok(
                tripPlannerService.plan(tenantId, origin, destination, legs, startDate));
    }
}