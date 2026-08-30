package com.ticketmesh.controller;

import com.ticketmesh.exception.NotFoundException;
import com.ticketmesh.model.ProviderProduct;
import com.ticketmesh.repository.ProviderProductRepository;
import com.ticketmesh.service.PricingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Transparent price breakdowns with optional promo code and currency
 * conversion for any product.
 */
@RestController
@RequestMapping("/api/pricing")
public class PricingController {

    private final PricingService pricingService;
    private final ProviderProductRepository productRepository;
    private final RequestContext requestContext;

    public PricingController(PricingService pricingService,
                             ProviderProductRepository productRepository,
                             RequestContext requestContext) {
        this.pricingService = pricingService;
        this.productRepository = productRepository;
        this.requestContext = requestContext;
    }

    @GetMapping("/{productId}")
    public ResponseEntity<PricingService.Breakdown> breakdown(
            @PathVariable("productId") Long productId,
            @RequestParam(value = "promoCode", required = false) String promoCode,
            @RequestParam(value = "currency", required = false) String currency) {
        ProviderProduct product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found: " + productId));
        String target = currency != null ? currency : product.getCurrencyIso();
        return ResponseEntity.ok(pricingService.breakdown(
                product, promoCode, target, requestContext.currentTenantId()));
    }
}