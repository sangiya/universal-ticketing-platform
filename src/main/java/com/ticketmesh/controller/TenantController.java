package com.ticketmesh.controller;

import com.ticketmesh.dto.BrandingRequest;
import com.ticketmesh.dto.BrandingResponse;
import com.ticketmesh.dto.TenantRequest;
import com.ticketmesh.dto.TenantResponse;
import com.ticketmesh.service.BrandingService;
import com.ticketmesh.service.TenantService;
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
public class TenantController {

    private final TenantService tenantService;
    private final BrandingService brandingService;

    public TenantController(TenantService tenantService, BrandingService brandingService) {
        this.tenantService = tenantService;
        this.brandingService = brandingService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/tenants")
    public ResponseEntity<TenantResponse> createTenant(
            @Valid @RequestBody TenantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tenantService.create(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/tenants")
    public ResponseEntity<List<TenantResponse>> listTenants() {
        return ResponseEntity.ok(tenantService.list());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/tenants/{slug}")
    public ResponseEntity<TenantResponse> getTenant(@PathVariable("slug") String slug) {
        return ResponseEntity.ok(tenantService.getBySlug(slug));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/tenants/{slug}")
    public ResponseEntity<TenantResponse> updateTenant(
            @PathVariable("slug") String slug, @Valid @RequestBody TenantRequest request) {
        return ResponseEntity.ok(tenantService.update(slug, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/tenants/{slug}/status")
    public ResponseEntity<TenantResponse> setTenantEnabled(
            @PathVariable("slug") String slug,
            @RequestParam("enabled") boolean enabled) {
        return ResponseEntity.ok(tenantService.setEnabled(slug, enabled));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/tenants/{slug}/moderation")
    public ResponseEntity<TenantResponse> setTenantModeration(
            @PathVariable("slug") String slug,
            @RequestParam("mode") com.ticketmesh.model.Tenant.ModerationMode mode) {
        return ResponseEntity.ok(tenantService.setModerationMode(slug, mode));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/tenants/{slug}/branding")
    public ResponseEntity<BrandingResponse> upsertBranding(
            @PathVariable("slug") String slug, @Valid @RequestBody BrandingRequest request) {
        return ResponseEntity.ok(brandingService.upsert(slug, request));
    }

    @GetMapping("/tenant/{slug}/branding")
    public ResponseEntity<BrandingResponse> publicBranding(@PathVariable("slug") String slug) {
        return ResponseEntity.ok(brandingService.getPublic(slug));
    }
}
