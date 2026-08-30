package com.ticketmesh.controller;

import com.ticketmesh.config.capability.Capability;
import com.ticketmesh.config.capability.CapabilityCatalog;
import com.ticketmesh.config.domain.DomainDefinition;
import com.ticketmesh.config.domain.DomainRegistry;
import com.ticketmesh.config.product.ProductCatalogBuilder;
import com.ticketmesh.config.product.ProductTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

/**
 * Platform configuration surface. Exposes the config-driven verticals,
 * the explicit capability matrix and the no-code product templates - all
 * read-only, so any vertical can be on-boarded and audited from config.
 */
@RestController
@RequestMapping("/api/platform")
public class PlatformConfigController {

    private final DomainRegistry domainRegistry;
    private final CapabilityCatalog capabilityCatalog;
    private final ProductCatalogBuilder productCatalogBuilder;

    public PlatformConfigController(DomainRegistry domainRegistry,
                                    CapabilityCatalog capabilityCatalog,
                                    ProductCatalogBuilder productCatalogBuilder) {
        this.domainRegistry = domainRegistry;
        this.capabilityCatalog = capabilityCatalog;
        this.productCatalogBuilder = productCatalogBuilder;
    }

    @GetMapping("/domains")
    public ResponseEntity<List<DomainDefinition>> domains() {
        return ResponseEntity.ok(domainRegistry.list());
    }

    @GetMapping("/capabilities")
    public ResponseEntity<List<Capability>> capabilities(
            @RequestParam(value = "domain", required = false) String domain) {
        return ResponseEntity.ok(capabilityCatalog.capabilitiesFor(domain));
    }

    @GetMapping("/product-templates")
    public ResponseEntity<?> productTemplates(
            @RequestParam(value = "kind", required = false) String kind) {
        if (kind == null || kind.isBlank()) {
            return ResponseEntity.ok(productCatalogBuilder.allTemplates());
        }
        Optional<ProductTemplate> template = productCatalogBuilder.templateFor(kind);
        return template.isPresent()
                ? ResponseEntity.ok(template.get())
                : ResponseEntity.notFound().build();
    }
}