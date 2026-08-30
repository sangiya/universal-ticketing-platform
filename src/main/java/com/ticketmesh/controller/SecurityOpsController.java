package com.ticketmesh.controller;

import com.ticketmesh.dto.FraudCheckRequest;
import com.ticketmesh.dto.FraudCheckResponse;
import com.ticketmesh.dto.FraudSignalResponse;
import com.ticketmesh.model.FraudSignal;
import com.ticketmesh.service.FraudDetectionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Security & operations surface: fraud/risk evaluation for transactions, recent
 * fraud signals, and high-risk counters used by the ops/support team and the
 * automated monitoring.
 */
@RestController
@RequestMapping("/api")
public class SecurityOpsController {

    private final FraudDetectionService fraudDetectionService;

    public SecurityOpsController(FraudDetectionService fraudDetectionService) {
        this.fraudDetectionService = fraudDetectionService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/security/fraud/check")
    public ResponseEntity<FraudCheckResponse> check(
            @Valid @RequestBody FraudCheckRequest request) {
        return ResponseEntity.ok(fraudDetectionService.evaluate(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/security/fraud/signals")
    public ResponseEntity<List<FraudSignalResponse>> recent(
            @RequestParam("tenantId") Long tenantId) {
        return fraudDetectionService.recent(tenantId).stream()
                .map(this::toResponse).collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toList(), ResponseEntity::ok));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/security/fraud/high-count")
    public ResponseEntity<Long> highCount() {
        return ResponseEntity.ok(fraudDetectionService.countHighRisk());
    }

    @GetMapping("/health/live")
    public ResponseEntity<java.util.Map<String, Object>> liveness() {
        return ResponseEntity.ok(java.util.Map.of("status", "UP", "service", "ticketmesh-core"));
    }

    private FraudSignalResponse toResponse(FraudSignal s) {
        return new FraudSignalResponse(
                s.getId(), s.getTenantId(), s.getSubjectType(), s.getSubjectRef(),
                s.getActorUsername(), s.getScore(), s.getRisk().name(), s.getFlags(),
                s.getDetails(), s.getCreatedAt());
    }
}
