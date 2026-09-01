package com.ticketmesh.controller;

import com.ticketmesh.dto.PromotionRequest;
import com.ticketmesh.dto.PromotionToggleRequest;
import com.ticketmesh.model.Promotion;
import com.ticketmesh.service.PromotionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Promotion engine administration: create configurable coupon rules per
 * tenant, list them and toggle activation without deleting history.
 */
@RestController
@RequestMapping("/api/promotions")
public class PromotionController {

    private final PromotionService promotionService;
    private final RequestContext requestContext;

    public PromotionController(PromotionService promotionService,
                               RequestContext requestContext) {
        this.promotionService = promotionService;
        this.requestContext = requestContext;
    }

    @PostMapping
    public ResponseEntity<Promotion> create(@RequestBody PromotionRequest request) {
        Promotion.DiscountType type = Promotion.DiscountType.valueOf(
                request.discountType() == null ? "FLAT" : request.discountType().toUpperCase());
        Promotion.Kind kind = Promotion.Kind.valueOf(
                request.kind() == null ? "PROMO" : request.kind().toUpperCase());
        return ResponseEntity.status(HttpStatus.CREATED).body(promotionService.create(
                requestContext.tenantIdOr(request.tenantId()), request.code(), request.name(),
                type, request.discountValue(), request.minPurchase(), request.startsAt(),
                request.endsAt(), request.maxUses(), request.domains(), kind));
    }

    @GetMapping
    public ResponseEntity<List<Promotion>> list(
            @RequestParam(value = "tenantId", required = false) Long tenantId) {
        return ResponseEntity.ok(promotionService.listByTenant(requestContext.tenantIdOr(tenantId)));
    }

    /**
     * Public listing of all currently-active discount codes across all tenants.
     * No authentication required — visitors can browse available coupons before signing in.
     */
    @GetMapping("/active")
    public ResponseEntity<List<Promotion>> listActive() {
        return ResponseEntity.ok(promotionService.listPublicActive());
    }

    @PostMapping("/{id}/toggle")
    public ResponseEntity<Promotion> toggle(
            @PathVariable("id") Long promotionId,
            @RequestBody PromotionToggleRequest request) {
        return ResponseEntity.ok(promotionService.setEnabled(promotionId, request.enabled()));
    }
}