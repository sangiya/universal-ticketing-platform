package com.ticketmesh.controller;

import com.ticketmesh.exception.NotFoundException;
import com.ticketmesh.ml.DemandForecast;
import com.ticketmesh.ml.DynamicPriceProjection;
import com.ticketmesh.ml.DynamicPricingEngine;
import com.ticketmesh.ml.MlSuiteService;
import com.ticketmesh.ml.PricePrediction;
import com.ticketmesh.ml.PriceProjection;
import com.ticketmesh.ml.SeatRecommendation;
import com.ticketmesh.model.ProviderProduct;
import com.ticketmesh.repository.ProviderProductRepository;
import com.ticketmesh.service.PricingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Transparent price breakdowns with optional promo code and currency
 * conversion for any product. Also exposes the dynamic-pricing engine as an
 * advisory signal (spec §54).
 */
@RestController
@RequestMapping("/api/pricing")
public class PricingController {

    private final PricingService pricingService;
    private final ProviderProductRepository productRepository;
    private final RequestContext requestContext;
    private final DynamicPricingEngine dynamicPricingEngine;
    private final MlSuiteService mlSuiteService;

    public PricingController(PricingService pricingService,
                             ProviderProductRepository productRepository,
                             RequestContext requestContext,
                             DynamicPricingEngine dynamicPricingEngine,
                             MlSuiteService mlSuiteService) {
        this.pricingService = pricingService;
        this.productRepository = productRepository;
        this.requestContext = requestContext;
        this.dynamicPricingEngine = dynamicPricingEngine;
        this.mlSuiteService = mlSuiteService;
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

    /**
     * Dynamic price projection (spec §54). Computes a recommended price given
     * the current demand/capacity signals. Advisory only — never overrides the
     * base price silently.
     */
    @GetMapping("/{productId}/dynamic")
    public ResponseEntity<DynamicPriceProjection> dynamic(
            @PathVariable("productId") Long productId,
            @RequestParam(value = "demandScore", required = false, defaultValue = "50") int demandScore,
            @RequestParam(value = "capacityTotal", required = false) Integer capacityTotal) {
        ProviderProduct product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found: " + productId));
        int total = capacityTotal != null && capacityTotal > 0
                ? capacityTotal
                : Math.max(1, product.getAvailableQuantity());
        return ResponseEntity.ok(dynamicPricingEngine.projectedPrice(
                product.getPrice(), demandScore, product.getAvailableQuantity(), total));
    }

    /**
     * Demand forecast (spec §54) for the product over a horizon of days.
     */
    @GetMapping("/{productId}/forecast")
    public ResponseEntity<DemandForecast> forecast(
            @PathVariable("productId") Long productId,
            @RequestParam(value = "horizonDays", required = false, defaultValue = "30") int horizonDays) {
        return ResponseEntity.ok(mlSuiteService.demandForecast(productId, horizonDays));
    }

    /**
     * Price prediction (spec §54) for a given target date.
     */
    @GetMapping("/{productId}/prediction")
    public ResponseEntity<PricePrediction> prediction(
            @PathVariable("productId") Long productId,
            @RequestParam(value = "horizonDays", required = false, defaultValue = "7") int horizonDays) {
        return ResponseEntity.ok(mlSuiteService.predictPrice(productId, horizonDays));
    }

    /**
     * Price projection for a target sell-by date (spec §54).
     * Returns the series of predicted prices up to the target days horizon.
     */
    @GetMapping("/{productId}/projection")
    public ResponseEntity<List<PriceProjection>> projection(
            @PathVariable("productId") Long productId,
            @RequestParam(value = "targetDays", required = false, defaultValue = "14") int targetDays) {
        PricePrediction prediction = mlSuiteService.predictPrice(productId, targetDays);
        return ResponseEntity.ok(prediction.projections());
    }

    /**
     * Group / accessibility seat recommendations (spec §18).
     */
    @GetMapping("/{productId}/seats")
    public ResponseEntity<SeatRecommendation> seatRecommendation(
            @PathVariable("productId") Long productId,
            @RequestParam(value = "groupSize", required = false, defaultValue = "1") int groupSize,
            @RequestParam(value = "needsAccessibility", required = false, defaultValue = "false") boolean needsAccessibility) {
        String preference = needsAccessibility ? "AISLE" : "BALANCED";
        return ResponseEntity.ok(mlSuiteService.recommendSeats(groupSize, 50, preference, null));
    }
}
