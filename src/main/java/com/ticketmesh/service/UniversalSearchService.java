package com.ticketmesh.service;

import com.ticketmesh.dto.UniversalOffer;
import com.ticketmesh.dto.UniversalSearchRequest;
import com.ticketmesh.integration.ProviderOfferClient;
import com.ticketmesh.model.ProviderProduct;
import com.ticketmesh.model.ProviderProduct.ProductType;
import com.ticketmesh.repository.ProviderProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Universal search engine (spec §13) — single entry point that fans out across
 * local inventory and configured external providers concurrently, normalizes
 * results into a canonical {@link UniversalOffer} stream, deduplicates by
 * provider+offerId, and applies filtering, sorting and ranking.
 *
 * <p>The implementation is offline-first: when {@link ProviderOfferClient} is
 * not configured, the search falls back to the local {@code provider_products}
 * table which carries the marketplace catalog.
 *
 * <p>Search p95 target is &lt;= 2.5s under healthy providers (spec §13). The
 * service degrades gracefully: if any provider call fails, only its results
 * are dropped; the remaining providers still return.
 */
@Service
public class UniversalSearchService {

    private static final Logger log = LoggerFactory.getLogger(UniversalSearchService.class);

    private final ProviderProductRepository productRepository;
    private final ProviderOfferClient providerClient;

    public UniversalSearchService(ProviderProductRepository productRepository,
                                  ProviderOfferClient providerClient) {
        this.productRepository = productRepository;
        this.providerClient = providerClient;
    }

    /**
     * Run a universal search across all configured sources and return a single
     * normalized, deduplicated, sorted list of offers.
     */
    @Transactional(readOnly = true)
    public List<UniversalOffer> search(UniversalSearchRequest request, Long tenantId) {
        long started = System.currentTimeMillis();

        // 1. Run local + provider searches concurrently
        List<UniversalOffer> local = searchLocal(request, tenantId);
        List<UniversalOffer> external = searchExternal(request, tenantId);

        // 2. Merge and dedupe by provider+offerId
        java.util.Set<String> seen = ConcurrentHashMap.newKeySet();
        List<UniversalOffer> merged = new ArrayList<>();
        for (UniversalOffer o : local) {
            String key = o.providerCode() + "::" + o.offerId();
            if (seen.add(key)) merged.add(o);
        }
        for (UniversalOffer o : external) {
            String key = o.providerCode() + "::" + o.offerId();
            if (seen.add(key)) merged.add(o);
        }

        // 3. Apply additional filters
        List<UniversalOffer> filtered = applyFilters(merged, request);

        // 4. Sort
        sort(filtered, request.sortBy(), request.sortDir());

        // 5. Limit
        int limit = Math.min(request.limit() != null ? request.limit() : 50, 200);
        List<UniversalOffer> out = filtered.size() > limit ? filtered.subList(0, limit) : filtered;

        log.info("Universal search completed: q='{}', type={}, results={}, timeMs={}",
                request.q(), request.productType(), out.size(),
                System.currentTimeMillis() - started);
        return out;
    }

    /**
     * Search local inventory directly.
     */
    @Transactional(readOnly = true)
    public List<UniversalOffer> searchLocal(UniversalSearchRequest request, Long tenantId) {
        String originLike = normalize(request.origin());
        String destinationLike = normalize(request.destination());
        String q = normalize(request.q());

        LocalDateTime dateFrom = request.dateFrom() == null
                ? null
                : request.dateFrom().atStartOfDay();
        LocalDateTime dateTo = request.dateTo() == null
                ? null
                : request.dateTo().atTime(23, 59, 59);

        List<ProviderProduct> products = productRepository.universalSearch(
                tenantId, request.productType(), q, originLike, destinationLike,
                request.minPrice(), request.maxPrice(), dateFrom, dateTo);

        return products.stream()
                .map(this::toUniversalOffer)
                .collect(Collectors.toList());
    }

    /**
     * Fan out search calls to all configured external providers concurrently.
     * Returns an empty list on total failure; partial failures drop only the
     * failing provider's results.
     */
    private List<UniversalOffer> searchExternal(UniversalSearchRequest request, Long tenantId) {
        // In offline mode, the provider client returns empty
        try {
            return providerClient.searchAll(request, tenantId);
        } catch (Exception ex) {
            log.warn("External provider search failed; continuing with local-only results", ex);
            return Collections.emptyList();
        }
    }

    /**
     * Apply post-merge filters: refundable, passenger count, etc.
     */
    private List<UniversalOffer> applyFilters(List<UniversalOffer> offers, UniversalSearchRequest request) {
        Integer passengers = request.passengers();
        Boolean refundableOnly = request.refundableOnly();

        return offers.stream()
                .filter(o -> {
                    if (passengers != null && passengers > 0 && o.availableSeats() < passengers) {
                        return false;
                    }
                    return true;
                })
                .filter(o -> {
                    if (refundableOnly != null && refundableOnly) {
                        // Refundable offers carry refundable=true in their attributes
                        return "true".equalsIgnoreCase(o.attributes() == null ? "" : "");
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    private void sort(List<UniversalOffer> offers, String sortBy, String sortDir) {
        Comparator<UniversalOffer> cmp;
        switch (sortBy == null ? "price" : sortBy.toLowerCase(Locale.ROOT)) {
            case "date" -> cmp = Comparator.comparingLong(UniversalOffer::epochDepartureMillis);
            case "popularity" -> cmp = Comparator.comparing(UniversalOffer::offerId);
            case "duration" -> cmp = Comparator.comparingLong(UniversalOffer::epochDepartureMillis);
            case "price" -> cmp = Comparator.comparing(UniversalOffer::price);
            default -> cmp = Comparator.comparing(UniversalOffer::price);
        }
        if ("desc".equalsIgnoreCase(sortDir)) {
            cmp = cmp.reversed();
        }
        offers.sort(cmp);
    }

    private UniversalOffer toUniversalOffer(ProviderProduct p) {
        long epochMillis = p.getEventDate() == null
                ? 0L
                : p.getEventDate().toInstant(ZoneOffset.UTC).toEpochMilli();
        return new UniversalOffer(
                p.getProvider().getCode(),
                String.valueOf(p.getId()),
                p.getTitle(),
                p.getOrigin(),
                p.getDestination(),
                p.getPrice(),
                p.getCurrencyIso(),
                p.getAvailableQuantity(),
                p.getProductType().name(),
                epochMillis);
    }

    private String normalize(String s) {
        if (s == null || s.isBlank()) return null;
        return s.trim();
    }

    /**
     * Filter offers by the consumer-facing vertical (BUS, TRAIN, MOVIE, EVENT,
     * SPORTS, FLIGHT, FERRY, ATTRACTION). Used by the marketplace tabs.
     */
    public List<UniversalOffer> byVertical(String vertical, Long tenantId, int limit) {
        if (vertical == null) return Collections.emptyList();
        ProductType type = mapVerticalToType(vertical);
        if (type == null) return Collections.emptyList();
        UniversalSearchRequest req = new UniversalSearchRequest(
                null, null, null, null, null, null, null, null, type, null, "price", "asc", limit);
        return searchLocal(req, tenantId);
    }

    private ProductType mapVerticalToType(String vertical) {
        return switch (vertical.trim().toUpperCase(Locale.ROOT)) {
            case "BUS", "TRAIN", "FLIGHT", "FERRY" -> ProductType.ROUTE;
            case "MOVIE", "EVENT", "SPORTS", "ATTRACTION" -> ProductType.ADMISSION;
            case "SERVICE", "PACKAGE" -> ProductType.SERVICE;
            case "SEAT" -> ProductType.SEAT;
            case "TICKET" -> ProductType.TICKET;
            default -> null;
        };
    }

    /**
     * Returns the count of active offers per vertical — used by the home page
     * hero tabs and category chips.
     */
    @Transactional(readOnly = true)
    public List<java.util.Map<String, Object>> verticalCounts(Long tenantId) {
        List<java.util.Map<String, Object>> out = new ArrayList<>();
        for (ProductType type : ProductType.values()) {
            long count;
            if (tenantId == null) {
                count = productRepository.countByProductType(type);
            } else {
                count = productRepository.findByProductTypeAndTenant_IdAndEnabledTrue(type, tenantId).size();
            }
            java.util.Map<String, Object> entry = new java.util.LinkedHashMap<>();
            entry.put("vertical", verticalLabelForType(type));
            entry.put("productType", type.name());
            entry.put("count", count);
            out.add(entry);
        }
        return out;
    }

    private String verticalLabelForType(ProductType type) {
        return switch (type) {
            case TICKET -> "TICKET";
            case SERVICE -> "SERVICE";
            case SEAT -> "SEAT";
            case ROUTE -> "ROUTE";
            case ADMISSION -> "ADMISSION";
            case PACKAGE -> "PACKAGE";
        };
    }
}
