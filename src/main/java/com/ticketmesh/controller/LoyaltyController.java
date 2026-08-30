package com.ticketmesh.controller;

import com.ticketmesh.model.LoyaltyAccount;
import com.ticketmesh.service.LoyaltyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Customer loyalty: expose the authenticated customer's account with current
 * points and tier.
 */
@RestController
@RequestMapping("/api/loyalty")
public class LoyaltyController {

    private final LoyaltyService loyaltyService;
    private final RequestContext requestContext;

    public LoyaltyController(LoyaltyService loyaltyService, RequestContext requestContext) {
        this.loyaltyService = loyaltyService;
        this.requestContext = requestContext;
    }

    @GetMapping
    public ResponseEntity<LoyaltyAccount> mine() {
        return ResponseEntity.ok(loyaltyService.getOrCreate(
                requestContext.currentTenantId(), requestContext.currentUserId()));
    }
}