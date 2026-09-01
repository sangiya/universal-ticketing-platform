package com.ticketmesh.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketmesh.model.Provider;
import com.ticketmesh.repository.ProviderRepository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WireMock-backed stub server for all external provider types. Each provider
 * type (bus, train, flight, movie, event, sports, ferry, attraction) has its
 * own mock endpoint that returns realistic sample data when called via
 * {@link ProviderOfferClient}. The stub server runs embedded inside the test JVM.
 *
 * <p>Usage in tests:</p>
 * <pre>
 * ProviderStubServer stub = new ProviderStubServer(objectMapper);
 * stub.start();
 * try {
 *     // register providers, configure ProviderOfferClient, run tests
 * } finally {
 *     stub.stop();
 * }
 * </pre>
 */
public class ProviderStubServer {

    private final ObjectMapper objectMapper;
    private final ProviderRepository providerRepository;
    private final Map<String, Provider> registeredProviders = new ConcurrentHashMap<>();
    private com.github.tomakehurst.wiremock.WireMockServer wireMockServer;

    public ProviderStubServer(ObjectMapper objectMapper, ProviderRepository providerRepository) {
        this.objectMapper = objectMapper;
        this.providerRepository = providerRepository;
    }

    public void start() {
        if (wireMockServer != null && wireMockServer.isRunning()) return;
        wireMockServer = new com.github.tomakehurst.wiremock.WireMockServer(
                new com.github.tomakehurst.wiremock.core.WireMockConfiguration()
                        .dynamicPort()
                        .enableBrowserProxying(false)
                        .stubCorsEnabled(false));
        wireMockServer.start();
        configureStubs();
    }

    public void stop() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
        }
    }

    public int port() {
        return wireMockServer != null ? wireMockServer.port() : 0;
    }

    public String baseUrl() {
        return "http://localhost:" + port();
    }

    public void registerProvider(Provider provider, Provider.Status status) {
        String code = provider.getCode();
        String endpoint = baseUrl() + "/" + code;
        Provider saved = providerRepository.save(provider);
        saved.setStatus(status);
        saved.setApiEndpoint(endpoint);
        Provider stub = providerRepository.save(saved);
        registeredProviders.put(code, stub);
    }

    private void configureStubs() {
        if (wireMockServer == null) return;

        // Bus
        wireMockServer.stubFor(
                com.github.tomakehurst.wiremock.client.WireMock.get(
                        com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching("/BUS/search"))
                        .willReturn(
                                com.github.tomakehurst.wiremock.client.WireMock.aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(toJson(Map.of("offers", buildBusOffers())))));
        // Train
        wireMockServer.stubFor(
                com.github.tomakehurst.wiremock.client.WireMock.get(
                        com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching("/TRAIN/search"))
                        .willReturn(
                                com.github.tomakehurst.wiremock.client.WireMock.aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(toJson(Map.of("offers", buildTrainOffers())))));
        // Flight
        wireMockServer.stubFor(
                com.github.tomakehurst.wiremock.client.WireMock.get(
                        com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching("/FLIGHT/search"))
                        .willReturn(
                                com.github.tomakehurst.wiremock.client.WireMock.aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(toJson(Map.of("offers", buildFlightOffers())))));
        // Movie
        wireMockServer.stubFor(
                com.github.tomakehurst.wiremock.client.WireMock.get(
                        com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching("/MOVIE/search"))
                        .willReturn(
                                com.github.tomakehurst.wiremock.client.WireMock.aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(toJson(Map.of("offers", buildMovieOffers())))));
        // Event
        wireMockServer.stubFor(
                com.github.tomakehurst.wiremock.client.WireMock.get(
                        com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching("/EVENT/search"))
                        .willReturn(
                                com.github.tomakehurst.wiremock.client.WireMock.aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(toJson(Map.of("offers", buildEventOffers())))));
        // Sports
        wireMockServer.stubFor(
                com.github.tomakehurst.wiremock.client.WireMock.get(
                        com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching("/SPORTS/search"))
                        .willReturn(
                                com.github.tomakehurst.wiremock.client.WireMock.aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(toJson(Map.of("offers", buildSportsOffers())))));
        // Ferry
        wireMockServer.stubFor(
                com.github.tomakehurst.wiremock.client.WireMock.get(
                        com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching("/FERRY/search"))
                        .willReturn(
                                com.github.tomakehurst.wiremock.client.WireMock.aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(toJson(Map.of("offers", buildFerryOffers())))));
        // Attraction
        wireMockServer.stubFor(
                com.github.tomakehurst.wiremock.client.WireMock.get(
                        com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching("/ATTRACTION/search"))
                        .willReturn(
                                com.github.tomakehurst.wiremock.client.WireMock.aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(toJson(Map.of("offers", buildAttractionOffers())))));
        // Error
        wireMockServer.stubFor(
                com.github.tomakehurst.wiremock.client.WireMock.get(
                        com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching("/ERROR/search"))
                        .willReturn(
                                com.github.tomakehurst.wiremock.client.WireMock.aResponse()
                                        .withStatus(503)
                                        .withBody("{\"error\":\"Provider temporarily unavailable\"}")));
        // Slow (triggers circuit breaker)
        wireMockServer.stubFor(
                com.github.tomakehurst.wiremock.client.WireMock.get(
                        com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching("/SLOW/search"))
                        .willReturn(
                                com.github.tomakehurst.wiremock.client.WireMock.aResponse()
                                        .withStatus(200)
                                        .withFixedDelay(10_000)
                                        .withBody(toJson(Map.of("offers", java.util.List.of())))));
    }

    private java.util.List<Map<String, Object>> buildBusOffers() {
        return java.util.List.of(
                stubOffer("BUS001", "Colombo — Kandy Express", "Colombo", "Kandy", 450, "LKR", 42, "BUS"),
                stubOffer("BUS001", "Colombo — Galle Route", "Colombo", "Galle", 320, "LKR", 38, "BUS"));
    }

    private java.util.List<Map<String, Object>> buildTrainOffers() {
        return java.util.List.of(
                stubOffer("TRAIN001", "Podi Menike — Main Line", "Colombo Fort", "Kandy", 220, "LKR", 80, "TRAIN"),
                stubOffer("TRAIN001", "Southern Express", "Colombo Fort", "Galle", 300, "LKR", 55, "TRAIN"));
    }

    private java.util.List<Map<String, Object>> buildFlightOffers() {
        return java.util.List.of(
                stubOffer("FLIGHT001", "Colombo — Singapore SQ", "CMB", "SIN", 45000, "LKR", 180, "FLIGHT"),
                stubOffer("FLIGHT001", "Colombo — Dubai EK", "CMB", "DXB", 52000, "LKR", 220, "FLIGHT"));
    }

    private java.util.List<Map<String, Object>> buildMovieOffers() {
        return java.util.List.of(
                stubOffer("MOVIE001", "Dune: Part Two — IMAX 3D", "", "", 1800, "LKR", 200, "MOVIE"));
    }

    private java.util.List<Map<String, Object>> buildEventOffers() {
        return java.util.List.of(
                stubOffer("EVENT001", "Colombo Jazz Festival 2026", "", "", 5500, "LKR", 500, "EVENT"));
    }

    private java.util.List<Map<String, Object>> buildSportsOffers() {
        return java.util.List.of(
                stubOffer("SPORTS001", "SL vs India ODI", "", "", 2500, "LKR", 25000, "SPORTS"));
    }

    private java.util.List<Map<String, Object>> buildFerryOffers() {
        return java.util.List.of(
                stubOffer("FERRY001", "Jaffna — Delft Island Hop", "Jaffna Port", "Delft", 450, "LKR", 40, "FERRY"));
    }

    private java.util.List<Map<String, Object>> buildAttractionOffers() {
        return java.util.List.of(
                stubOffer("ATTR001", "Sigiriya Rock Fortress", "", "", 6500, "LKR", 500, "ATTRACTION"));
    }

    private Map<String, Object> stubOffer(String provider, String title, String origin,
                                          String destination, int price, String currency,
                                          int seats, String vertical) {
        return Map.ofEntries(
                Map.entry("providerCode", provider),
                Map.entry("offerId", title.hashCode() + "-" + System.nanoTime()),
                Map.entry("title", title),
                Map.entry("origin", origin),
                Map.entry("destination", destination),
                Map.entry("price", price),
                Map.entry("currency", currency),
                Map.entry("availableSeats", seats),
                Map.entry("vertical", vertical),
                Map.entry("departureEpochMillis", System.currentTimeMillis() + 86400000L));
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception ex) {
            return "{}";
        }
    }
}
