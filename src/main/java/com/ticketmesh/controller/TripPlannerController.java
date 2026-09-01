package com.ticketmesh.controller;

import com.ticketmesh.dto.TripPlan;
import com.ticketmesh.security.TenantGuard;
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
    private final TenantGuard tenantGuard;

    public TripPlannerController(TripPlannerService tripPlannerService, TenantGuard tenantGuard) {
        this.tripPlannerService = tripPlannerService;
        this.tenantGuard = tenantGuard;
    }

    @GetMapping("/plan")
    public ResponseEntity<TripPlan> plan(
            @RequestParam(name = "tenantId", required = false) Long tenantId,
            @RequestParam(name = "origin") String origin,
            @RequestParam(name = "destination") String destination,
            @RequestParam(name = "legs", defaultValue = "2") int legs,
            @RequestParam(name = "startDate", required = false) String startDate) {
        Long effectiveTenantId = tenantGuard.requireAccessTo(tenantId);
        return ResponseEntity.ok(
                tripPlannerService.plan(effectiveTenantId, origin, destination, legs, startDate));
    }
}