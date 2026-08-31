package com.ticketmesh.controller;

import com.ticketmesh.dto.AdminStatsResponse;
import com.ticketmesh.dto.OrderSummary;
import com.ticketmesh.dto.ShopResponse;
import com.ticketmesh.dto.UserSummary;
import com.ticketmesh.model.AgentShop;
import com.ticketmesh.service.AdminService;
import com.ticketmesh.service.AgentOnboardingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final AgentOnboardingService onboardingService;

    public AdminController(AdminService adminService,
                           AgentOnboardingService onboardingService) {
        this.adminService = adminService;
        this.onboardingService = onboardingService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<AdminStatsResponse> dashboard() {
        return ResponseEntity.ok(adminService.dashboard());
    }

    @GetMapping("/shops")
    public ResponseEntity<List<ShopResponse>> listShops(
            @RequestParam("status") AgentShop.Status status) {
        return ResponseEntity.ok(onboardingService.listByStatus(status));
    }

    @PutMapping("/shops/{id}")
    public ResponseEntity<ShopResponse> reviewShop(
            @PathVariable("id") Long id,
            @RequestParam("action") AgentShop.Status action) {
        return ResponseEntity.ok(onboardingService.approve(id, action));
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserSummary>> listUsers() {
        return ResponseEntity.ok(adminService.listUsers());
    }

    @GetMapping("/orders")
    public ResponseEntity<List<OrderSummary>> listOrders() {
        return ResponseEntity.ok(adminService.listOrders());
    }
}
