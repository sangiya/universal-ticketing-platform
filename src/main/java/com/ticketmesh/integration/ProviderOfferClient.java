package com.ticketmesh.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketmesh.dto.UniversalOffer;
import com.ticketmesh.dto.UniversalSearchRequest;
import com.ticketmesh.model.Provider;
import com.ticketmesh.repository.ProviderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Generic outbound HTTP client used to call an external provider's search API.
 * It applies a hard timeout, maps the provider response (through its raw
 * adapter contract) into the canonical {@link UniversalOffer} model, and treats
 * non-2xx responses as unavailable offers. In tests the provider endpoint is
 * stubbed with WireMock so the whole integration is deterministic and offline.
 */
@Component
public class ProviderOfferClient {

    private static final Logger log = LoggerFactory.getLogger(ProviderOfferClient.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;
    private final ProviderRepository providerRepository;
    private final ProviderCircuitBreaker circuitBreaker;
    private final ExecutorService executor;

    @Autowired
    public ProviderOfferClient(ObjectMapper objectMapper,
                               ProviderRepository providerRepository,
                               ProviderCircuitBreaker circuitBreaker,
                               @Value("${app.provider.connect-timeout-ms:2000}") int connectTimeoutMillis,
                               @Value("${app.provider.read-timeout-ms:3000}") int readTimeoutMillis) {
        this.objectMapper = objectMapper;
        this.providerRepository = providerRepository;
        this.circuitBreaker = circuitBreaker;
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMillis))
                .build();
        this.executor = Executors.newFixedThreadPool(
                Math.max(2, Runtime.getRuntime().availableProcessors()),
                r -> {
                    Thread t = new Thread(r, "provider-search");
                    t.setDaemon(true);
                    return t;
                });
    }

    /**
     * Test-only constructor — circuit breaker omitted for simple unit tests.
     */
    public ProviderOfferClient(ObjectMapper objectMapper,
                               ProviderRepository providerRepository,
                               int connectTimeoutMillis,
                               int readTimeoutMillis) {
        this(objectMapper, providerRepository, null, connectTimeoutMillis, readTimeoutMillis);
    }

    public List<UniversalOffer> search(String endpoint,
                                       String origin, String destination,
                                       String travelDate) {
        String url = buildUrl(endpoint, origin, destination, travelDate);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(readTimeoutMillis))
                .header("Accept", "application/json")
                .GET()
                .build();

        try {
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Provider search returned HTTP {} from {}", response.statusCode(), url);
                return List.of();
            }
            return parseOffers(response.body());
        } catch (Exception ex) {
            log.warn("Provider search failed for {}: {}", endpoint, ex.getMessage());
            return List.of();
        }
    }

    /**
     * Fan out search calls to all configured active providers concurrently and
     * return a single normalized list of offers. The per-provider call is
     * independently timeout-bounded. A failure in one provider only drops its
     * own results.
     */
    public List<UniversalOffer> searchAll(UniversalSearchRequest request, Long tenantId) {
        if (providerRepository == null) {
            return Collections.emptyList();
        }
        List<Provider> active = providerRepository.findAll().stream()
                .filter(p -> p.getStatus() == Provider.Status.ACTIVE
                        && p.getApiEndpoint() != null
                        && !p.getApiEndpoint().isBlank())
                .toList();
        if (active.isEmpty()) {
            return Collections.emptyList();
        }

        List<CompletableFuture<List<UniversalOffer>>> futures = active.stream()
                .map(p -> CompletableFuture
                        .supplyAsync(() -> searchProvider(p, request), executor)
                        .orTimeout(readTimeoutMillis, TimeUnit.MILLISECONDS)
                        .exceptionally(ex -> {
                            log.warn("Provider {} search failed: {}", p.getCode(), ex.getMessage());
                            return Collections.emptyList();
                        }))
                .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    private List<UniversalOffer> searchProvider(Provider provider, UniversalSearchRequest request) {
        if (provider.getApiEndpoint() == null) {
            return Collections.emptyList();
        }
        if (circuitBreaker != null) {
            return circuitBreaker.searchWithBreaker(
                    provider,
                    request,
                    endpoint -> search(endpoint,
                            request.origin(), request.destination(),
                            request.dateFrom() == null ? null : request.dateFrom().toString()));
        }
        return search(provider.getApiEndpoint(),
                request.origin(), request.destination(),
                request.dateFrom() == null ? null : request.dateFrom().toString());
    }

    private List<UniversalOffer> parseOffers(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode offers = root.path("offers");
            List<UniversalOffer> result = new ArrayList<>();
            if (offers.isArray()) {
                for (JsonNode n : offers) {
                    result.add(new UniversalOffer(
                            n.path("providerCode").asText(""),
                            n.path("offerId").asText(),
                            n.path("title").asText(),
                            n.path("origin").asText(),
                            n.path("destination").asText(),
                            new BigDecimal(n.path("price").asText("0")),
                            n.path("currency").asText("LKR"),
                            n.path("availableSeats").asInt(0),
                            n.path("vertical").asText("OTHER"),
                            n.path("departureEpochMillis").asLong(0L),
                            n.path("durationMinutes").asInt(0),
                            n.path("refundable").asBoolean(false),
                            n.path("rating").asDouble(0.0),
                            n.has("attributes") && !n.path("attributes").isNull()
                                    ? n.path("attributes").toString()
                                    : null));
                }
            }
            return result;
        } catch (Exception ex) {
            log.warn("Failed to parse provider offers: {}", ex.getMessage());
            return List.of();
        }
    }

    private String buildUrl(String endpoint, String origin, String destination,
                            String travelDate) {
        String base = endpoint.endsWith("/") ? endpoint : endpoint + "/";
        return base + "search?origin=" + encode(origin)
                + "&destination=" + encode(destination)
                + "&date=" + encode(travelDate);
    }

    private String encode(String value) {
        try {
            return java.net.URLEncoder.encode(value == null ? "" : value,
                    java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception ex) {
            return "";
        }
    }
}
