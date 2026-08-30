package com.ticketmesh.controller;

import com.ticketmesh.dto.ShopApplyRequest;
import com.ticketmesh.dto.ShopResponse;
import com.ticketmesh.model.AgentShop;
import com.ticketmesh.service.AgentOnboardingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AgentController {

    private final AgentOnboardingService onboardingService;

    public AgentController(AgentOnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @PostMapping("/agent/shops")
    public ResponseEntity<ShopResponse> apply(
            @RequestParam("tenant") String tenantSlug,
            @Valid @RequestBody ShopApplyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(onboardingService.apply(tenantSlug, request));
    }

    @GetMapping("/agent/shops/me")
    public ResponseEntity<ShopResponse> myShop() {
        return ResponseEntity.ok(onboardingService.myShop());
    }
}
