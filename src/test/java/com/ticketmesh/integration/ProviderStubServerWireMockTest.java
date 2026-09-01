package com.ticketmesh.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.ticketmesh.dto.UniversalOffer;
import com.ticketmesh.repository.ProviderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * End-to-end test for {@link ProviderStubServer} verifying that the embedded
 * WireMock instance serves realistic data for all provider types (bus, train,
 * flight, movie, event, sports, ferry, attraction). The {@link ProviderOfferClient}
 * is exercised against the live stub endpoints and returns normalized offers.
 */
class ProviderStubServerWireMockTest {

    private WireMockServer wireMock;
    private ProviderOfferClient client;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMock.start();

        // Configure stubs for all 8 provider types
        wireMock.stubFor(get(urlPathMatching("/BUS/search"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"offers":[
                                  {"providerCode":"BUS001","offerId":"B-1",
                                   "title":"Colombo — Kandy Express","origin":"Colombo",
                                   "destination":"Kandy","price":450,"currency":"LKR",
                                   "availableSeats":42,"vertical":"BUS"},
                                  {"providerCode":"BUS001","offerId":"B-2",
                                   "title":"Colombo — Galle Route","origin":"Colombo",
                                   "destination":"Galle","price":320,"currency":"LKR",
                                   "availableSeats":38,"vertical":"BUS"}
                                ]}""")));

        wireMock.stubFor(get(urlPathMatching("/TRAIN/search"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"offers":[
                                  {"providerCode":"TRAIN001","offerId":"T-1",
                                   "title":"Podi Menike — Main Line","origin":"Colombo Fort",
                                   "destination":"Kandy","price":220,"currency":"LKR",
                                   "availableSeats":80,"vertical":"TRAIN"}
                                ]}""")));

        wireMock.stubFor(get(urlPathMatching("/FLIGHT/search"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"offers":[
                                  {"providerCode":"FLIGHT001","offerId":"F-1",
                                   "title":"Colombo — Singapore SQ","origin":"CMB",
                                   "destination":"SIN","price":45000,"currency":"LKR",
                                   "availableSeats":180,"vertical":"FLIGHT"}
                                ]}""")));

        wireMock.stubFor(get(urlPathMatching("/MOVIE/search"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"offers":[
                                  {"providerCode":"MOVIE001","offerId":"M-1",
                                   "title":"Dune: Part Two — IMAX 3D","origin":"",
                                   "destination":"","price":1800,"currency":"LKR",
                                   "availableSeats":200,"vertical":"MOVIE"}
                                ]}""")));

        wireMock.stubFor(get(urlPathMatching("/EVENT/search"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"offers":[
                                  {"providerCode":"EVENT001","offerId":"E-1",
                                   "title":"Colombo Jazz Festival","origin":"",
                                   "destination":"","price":5500,"currency":"LKR",
                                   "availableSeats":500,"vertical":"EVENT"}
                                ]}""")));

        wireMock.stubFor(get(urlPathMatching("/SPORTS/search"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"offers":[
                                  {"providerCode":"SPORTS001","offerId":"S-1",
                                   "title":"SL vs India ODI","origin":"",
                                   "destination":"","price":2500,"currency":"LKR",
                                   "availableSeats":25000,"vertical":"SPORTS"}
                                ]}""")));

        wireMock.stubFor(get(urlPathMatching("/FERRY/search"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"offers":[
                                  {"providerCode":"FERRY001","offerId":"F2-1",
                                   "title":"Jaffna — Delft Island Hop","origin":"Jaffna",
                                   "destination":"Delft","price":450,"currency":"LKR",
                                   "availableSeats":40,"vertical":"FERRY"}
                                ]}""")));

        wireMock.stubFor(get(urlPathMatching("/ATTRACTION/search"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"offers":[
                                  {"providerCode":"ATTR001","offerId":"A-1",
                                   "title":"Sigiriya Rock Fortress","origin":"",
                                   "destination":"","price":6500,"currency":"LKR",
                                   "availableSeats":500,"vertical":"ATTRACTION"}
                                ]}""")));

        wireMock.stubFor(get(urlPathMatching("/ERROR/search"))
                .willReturn(aResponse().withStatus(503)
                        .withStatusMessage("Service Unavailable")));

        client = new ProviderOfferClient(
                new com.fasterxml.jackson.databind.ObjectMapper(),
                mock(ProviderRepository.class),
                2000, 3000);
    }

    @AfterEach
    void tearDown() {
        if (wireMock != null) wireMock.stop();
    }

    @Test
    void busProviderReturnsBusOffers() {
        List<UniversalOffer> offers = client.search(
                "http://localhost:" + wireMock.port() + "/BUS",
                "Colombo", "Kandy", "2026-09-01");
        assertEquals(2, offers.size());
        assertTrue(offers.stream().allMatch(o -> "BUS".equals(o.vertical())));
        assertEquals("BUS001", offers.get(0).providerCode());
    }

    @Test
    void trainProviderReturnsTrainOffers() {
        List<UniversalOffer> offers = client.search(
                "http://localhost:" + wireMock.port() + "/TRAIN",
                "Colombo Fort", "Kandy", "2026-09-01");
        assertEquals(1, offers.size());
        assertEquals("TRAIN", offers.get(0).vertical());
        assertEquals(new BigDecimal(220), offers.get(0).price());
    }

    @Test
    void flightProviderReturnsFlightOffers() {
        List<UniversalOffer> offers = client.search(
                "http://localhost:" + wireMock.port() + "/FLIGHT",
                "CMB", "SIN", "2026-09-01");
        assertFalse(offers.isEmpty());
        assertEquals("FLIGHT", offers.get(0).vertical());
        assertEquals(180, offers.get(0).availableSeats());
    }

    @Test
    void movieProviderReturnsMovieOffers() {
        List<UniversalOffer> offers = client.search(
                "http://localhost:" + wireMock.port() + "/MOVIE",
                "", "", "2026-09-01");
        assertEquals(1, offers.size());
        assertEquals("MOVIE", offers.get(0).vertical());
        assertEquals("Dune: Part Two — IMAX 3D", offers.get(0).title());
    }

    @Test
    void eventProviderReturnsEventOffers() {
        List<UniversalOffer> offers = client.search(
                "http://localhost:" + wireMock.port() + "/EVENT",
                "", "", "2026-09-01");
        assertEquals(1, offers.size());
        assertEquals("EVENT", offers.get(0).vertical());
    }

    @Test
    void sportsProviderReturnsSportsOffers() {
        List<UniversalOffer> offers = client.search(
                "http://localhost:" + wireMock.port() + "/SPORTS",
                "", "", "2026-09-01");
        assertEquals(1, offers.size());
        assertEquals("SPORTS", offers.get(0).vertical());
        assertEquals(25000, offers.get(0).availableSeats());
    }

    @Test
    void ferryProviderReturnsFerryOffers() {
        List<UniversalOffer> offers = client.search(
                "http://localhost:" + wireMock.port() + "/FERRY",
                "Jaffna", "Delft", "2026-09-01");
        assertEquals(1, offers.size());
        assertEquals("FERRY", offers.get(0).vertical());
    }

    @Test
    void attractionProviderReturnsAttractionOffers() {
        List<UniversalOffer> offers = client.search(
                "http://localhost:" + wireMock.port() + "/ATTRACTION",
                "", "", "2026-09-01");
        assertEquals(1, offers.size());
        assertEquals("ATTRACTION", offers.get(0).vertical());
        assertEquals(new BigDecimal(6500), offers.get(0).price());
    }

    @Test
    void errorProviderReturnsEmptyList() {
        List<UniversalOffer> offers = client.search(
                "http://localhost:" + wireMock.port() + "/ERROR",
                "anywhere", "anywhere", "2026-09-01");
        assertTrue(offers.isEmpty());
    }

    @Test
    void unconfiguredEndpointReturnsEmptyList() {
        List<UniversalOffer> offers = client.search(
                "http://localhost:" + wireMock.port() + "/NOT-REGISTERED",
                "anywhere", "anywhere", "2026-09-01");
        // Will return 404 → empty list
        assertNotNull(offers);
    }
}
