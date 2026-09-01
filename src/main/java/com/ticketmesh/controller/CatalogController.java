package com.ticketmesh.controller;

import com.ticketmesh.dto.CatalogSearchResult;
import com.ticketmesh.dto.ProductRequest;
import com.ticketmesh.dto.ProductResponse;
import com.ticketmesh.security.TenantGuard;
import com.ticketmesh.service.CatalogService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
public class CatalogController {

    private final CatalogService catalogService;
    private final TenantGuard tenantGuard;

    public CatalogController(CatalogService catalogService, TenantGuard tenantGuard) {
        this.catalogService = catalogService;
        this.tenantGuard = tenantGuard;
    }

    @PostMapping("/agent/providers/{code}/products")
    public ResponseEntity<ProductResponse> upload(
            @PathVariable("code") String providerCode,
            @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(catalogService.upload(providerCode, request));
    }

    @GetMapping("/agent/products")
    public ResponseEntity<List<ProductResponse>> myProducts() {
        return ResponseEntity.ok(catalogService.myProviderProducts());
    }

    @PutMapping("/agent/products/{id}")
    public ResponseEntity<ProductResponse> update(
            @PathVariable("id") Long id, @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(catalogService.update(id, request));
    }

    @PutMapping("/agent/products/{id}/status")
    public ResponseEntity<ProductResponse> setEnabled(
            @PathVariable("id") Long id, @RequestParam("enabled") boolean enabled) {
        return ResponseEntity.ok(catalogService.setEnabled(id, enabled));
    }

    @GetMapping("/catalog")
    public ResponseEntity<List<ProductResponse>> search(
            @RequestParam(value = "tenantId", required = false) Long tenantId,
            @RequestParam(value = "type", required = false) String productType) {
        Long effectiveTenantId = tenantGuard.requireAccessTo(tenantId);
        return ResponseEntity.ok(catalogService.searchCustomer(effectiveTenantId, productType));
    }

    @GetMapping("/catalog/search")
    public ResponseEntity<List<CatalogSearchResult>> search(
            @RequestParam(value = "q", required = false) String query) {
        return ResponseEntity.ok(catalogService.searchText(query));
    }

    @GetMapping("/catalog/{id}")
    public ResponseEntity<ProductResponse> getById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(catalogService.getById(id));
    }

    /**
     * Movies endpoint — only ADMISSION products with movie metadata.
     * Supports filters by language, genre, format, and section (now_showing / coming_soon / premieres).
     */
    @GetMapping("/movies")
    public ResponseEntity<List<ProductResponse>> movies(
            @RequestParam(value = "tenantId", required = false) Long tenantId,
            @RequestParam(value = "language", required = false) String language,
            @RequestParam(value = "genre", required = false) String genre,
            @RequestParam(value = "format", required = false) String format,
            @RequestParam(value = "section", required = false) String section) {
        Long effectiveTenantId = tenantGuard.requireAccessTo(tenantId);
        return ResponseEntity.ok(
                catalogService.listMovies(effectiveTenantId, language, genre, format, section));
    }
}
