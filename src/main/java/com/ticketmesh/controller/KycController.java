package com.ticketmesh.controller;

import com.ticketmesh.service.KycService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/kyc")
public class KycController {

    private final KycService kycService;

    public KycController(KycService kycService) {
        this.kycService = kycService;
    }

    @PostMapping("/submit")
    public ResponseEntity<?> submit(@RequestBody Map<String,String> body) {
        return ResponseEntity.ok(kycService.submit(body));
    }

    @GetMapping("/me")
    public ResponseEntity<List<Map<String,Object>>> my() {
        return ResponseEntity.ok(kycService.mySubmissions());
    }

    @GetMapping("/admin/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String,Object>>> pending() {
        return ResponseEntity.ok(kycService.pendingForAdmin());
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> review(@PathVariable Long id, @RequestBody Map<String,String> body) {
        return ResponseEntity.ok(kycService.review(id, body.get("action"), body.get("internalReason"), body.get("customerReason")));
    }
}
