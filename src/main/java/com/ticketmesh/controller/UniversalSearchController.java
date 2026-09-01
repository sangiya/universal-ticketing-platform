package com.ticketmesh.controller;

import com.ticketmesh.dto.UniversalOffer;
import com.ticketmesh.dto.UniversalSearchRequest;
import com.ticketmesh.service.UniversalSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Universal search console (spec §13) — single entry point for any customer's
 * "where do I want to go, when, and how many tickets?" question. Returns a
 * normalized, deduplicated, sorted list of offers across all enabled providers.
 *
 * <ul>
 *   <li>GET  /api/search              — query string, location, date, filters
 *   <li>POST /api/search              — same but with a typed request body
 *   <li>GET  /api/search/verticals    — list of verticals with counts
 *   <li>GET  /api/search/vertical/{v} — offers in a single vertical
 * </ul>
 */
@RestController
@RequestMapping("/api/search")
public class UniversalSearchController {

    private final UniversalSearchService searchService;

    public UniversalSearchController(UniversalSearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * GET form: query parameters (mobile web + browser console).
     */
    @GetMapping
    public ResponseEntity<List<UniversalOffer>> search(
            @RequestParam(value = "tenantId", required = false) Long tenantId,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "origin", required = false) String origin,
            @RequestParam(value = "destination", required = false) String destination,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "minPrice", required = false) java.math.BigDecimal minPrice,
            @RequestParam(value = "maxPrice", required = false) java.math.BigDecimal maxPrice,
            @RequestParam(value = "sortBy", required = false) String sortBy,
            @RequestParam(value = "sortDir", required = false) String sortDir,
            @RequestParam(value = "limit", required = false) Integer limit) {
        UniversalSearchRequest req = new UniversalSearchRequest(
                q, origin, destination, null, null, null,
                minPrice, maxPrice, parseType(type),
                null, sortBy, sortDir, limit);
        return ResponseEntity.ok(searchService.search(req, tenantId));
    }

    /**
     * POST form: typed request body (programmatic / SDK use).
     */
    @PostMapping
    public ResponseEntity<List<UniversalOffer>> searchPost(
            @RequestParam(value = "tenantId", required = false) Long tenantId,
            @RequestBody UniversalSearchRequest request) {
        return ResponseEntity.ok(searchService.search(request, tenantId));
    }

    /**
     * Browse by vertical — used by the home page category cards.
     */
    @GetMapping("/vertical/{vertical}")
    public ResponseEntity<List<UniversalOffer>> byVertical(
            @PathVariable("vertical") String vertical,
            @RequestParam(value = "tenantId", required = false) Long tenantId,
            @RequestParam(value = "limit", required = false, defaultValue = "20") Integer limit) {
        return ResponseEntity.ok(searchService.byVertical(vertical, tenantId, limit));
    }

    /**
     * List of verticals with offer counts — used by the hero tabs and category
     * chips on the home page.
     */
    @GetMapping("/verticals")
    public ResponseEntity<List<Map<String, Object>>> verticals(
            @RequestParam(value = "tenantId", required = false) Long tenantId) {
        return ResponseEntity.ok(searchService.verticalCounts(tenantId));
    }

    private com.ticketmesh.model.ProviderProduct.ProductType parseType(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return com.ticketmesh.model.ProviderProduct.ProductType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
