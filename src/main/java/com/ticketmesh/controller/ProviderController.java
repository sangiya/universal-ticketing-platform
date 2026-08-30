package com.ticketmesh.controller;

import com.ticketmesh.dto.ProviderConnectRequest;
import com.ticketmesh.dto.ProviderResponse;
import com.ticketmesh.model.Provider;
import com.ticketmesh.service.ProviderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ProviderController {

    private final ProviderService providerService;

    public ProviderController(ProviderService providerService) {
        this.providerService = providerService;
    }

    @PostMapping("/agent/providers")
    public ResponseEntity<ProviderResponse> connect(
            @Valid @RequestBody ProviderConnectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(providerService.connect(request));
    }

    @GetMapping("/agent/providers")
    public ResponseEntity<List<ProviderResponse>> mine() {
        return ResponseEntity.ok(providerService.mine());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/providers")
    public ResponseEntity<List<ProviderResponse>> listByStatus(
            @RequestParam("status") Provider.Status status) {
        return ResponseEntity.ok(providerService.listAll(status));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/providers/{id}/status")
    public ResponseEntity<ProviderResponse> setStatus(
            @PathVariable("id") Long id,
            @RequestParam("status") Provider.Status status) {
        return ResponseEntity.ok(providerService.setStatus(id, status));
    }
}
