package com.ticketmesh.controller;

import com.ticketmesh.repository.ProviderProductRepository;
import com.ticketmesh.repository.TenantRepository;
import com.ticketmesh.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class PublicStatsController {

    private final ProviderProductRepository productRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;

    public PublicStatsController(ProviderProductRepository productRepository,
                                 TenantRepository tenantRepository,
                                 UserRepository userRepository) {
        this.productRepository = productRepository;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/public/stats")
    public ResponseEntity<Map<String, Object>> getPublicStats() {
        return ResponseEntity.ok(Map.of(
            "liveOffers", productRepository.countByEnabledTrue(),
            "verticals", 9,
            "portals", 3,
            "availability", "24/7"
        ));
    }
}
