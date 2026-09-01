package com.ticketmesh.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.ticketmesh.dto.UniversalOffer;
import com.ticketmesh.repository.ProviderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Verifies the outbound provider adapter against an external ticket API that is
 * stubbed with WireMock, so tests are deterministic and run offline — no paid
 * or network provider APIs required.
 */
class ProviderOfferClientWireMockTest {

    private WireMockServer wireMock;
    private ProviderOfferClient client;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(options().dynamicPort());
        wireMock.start();
        client = new ProviderOfferClient(new ObjectMapper(), mock(ProviderRepository.class), 2000, 3000);
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Test
    void parsesNormalizedOffersFromStubbedProvider() {
        wireMock.stubFor(get(urlPathEqualTo("/v1/search"))
                .withQueryParam("origin", equalTo("Colombo"))
                .withQueryParam("destination", equalTo("Kandy"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("""
                                {"offers":[
                                  {"providerCode":"EXPRESS","offerId":"EX-1001",
                                   "title":"Colombo-Kandy Express","origin":"Colombo",
                                   "destination":"Kandy","price":"1250.00","currency":"LKR",
                                   "availableSeats":42,"vertical":"TRAIN","departureEpochMillis":1710000000000},
                                  {"providerCode":"EXPRESS","offerId":"EX-1002",
                                   "title":"Coastal Express","origin":"Colombo",
                                   "destination":"Kandy","price":"1150.00","currency":"LKR",
                                   "availableSeats":18,"vertical":"TRAIN","departureEpochMillis":1710003600000}
                                ]}""")));

        List<UniversalOffer> offers =
                client.search("http://localhost:" + wireMock.port() + "/v1", "Colombo", "Kandy", "2026-09-01");

        assertEquals(2, offers.size());
        UniversalOffer first = offers.get(0);
        assertEquals("EXPRESS", first.providerCode());
        assertEquals("EX-1001", first.offerId());
        assertEquals(1250.00d, first.price().doubleValue());
        assertEquals(42, first.availableSeats());
        assertEquals("TRAIN", first.vertical());
        assertEquals("Kandy", first.destination());
    }

    @Test
    void returnsEmptyListOnProviderError() {
        wireMock.stubFor(get(urlPathEqualTo("/v1/search"))
                .willReturn(aResponse().withStatus(500)));

        List<UniversalOffer> offers =
                client.search("http://localhost:" + wireMock.port() + "/v1", "Colombo", "Galle", "2026-09-01");

        assertTrue(offers.isEmpty());
    }
}
