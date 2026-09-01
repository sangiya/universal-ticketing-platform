package com.ticketmesh.ai;

import com.ticketmesh.ai.dto.SearchMatch;
import com.ticketmesh.ai.service.Intent;
import com.ticketmesh.ai.service.IntentService;
import com.ticketmesh.model.Provider;
import com.ticketmesh.model.ProviderProduct;
import com.ticketmesh.model.Tenant;
import com.ticketmesh.model.TrainRoute;
import com.ticketmesh.repository.TrainRouteRepository;
import com.ticketmesh.service.UniversalSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IntentServiceTest {

    private TrainRouteRepository routeRepository;
    private UniversalSearchService universalSearchService;
    private IntentService service;

    @BeforeEach
    void setUp() {
        routeRepository = mock(TrainRouteRepository.class);
        universalSearchService = mock(UniversalSearchService.class);
        service = new IntentService(routeRepository, universalSearchService);
    }

    @Test
    void detectIntent_routeQuestionsResolveToSearchRoutes() {
        assertEquals(Intent.SEARCH_ROUTES, service.detectIntent("Show me trains from Colombo to Kandy"));
        assertEquals(Intent.SEARCH_ROUTES, service.detectIntent("What is the schedule for the night express?"));
        assertEquals(Intent.SEARCH_ROUTES, service.detectIntent("Platform 2 departures this evening"));
    }

    @Test
    void detectIntent_priceQuestionsResolveToPriceCheck() {
        assertEquals(Intent.PRICE_CHECK, service.detectIntent("How much is the fare to Kandy?"));
        assertEquals(Intent.PRICE_CHECK, service.detectIntent("What does an express ticket cost?"));
    }

    @Test
    void detectIntent_bookingQuestionsResolveToBookingHelp() {
        assertEquals(Intent.BOOKING_HELP, service.detectIntent("I want to book a seat to Ella"));
        assertEquals(Intent.BOOKING_HELP, service.detectIntent("How do I reserve two tickets?"));
    }

    @Test
    void detectIntent_supportQuestionsResolveToSupport() {
        assertEquals(Intent.SUPPORT, service.detectIntent("Can you help with a refund?"));
        assertEquals(Intent.SUPPORT, service.detectIntent("My train was cancelled, who do I contact?"));
    }

    @Test
    void detectIntent_unknownOrBlankFallsBackToGeneral() {
        assertEquals(Intent.GENERAL, service.detectIntent("Tell me a joke"));
        assertEquals(Intent.GENERAL, service.detectIntent("  "));
        assertEquals(Intent.GENERAL, service.detectIntent(null));
    }

    @Test
    void searchEntity_matchesRouteNameOriginDestination() {
        TrainRoute kandyRoute = route(1L, "Colombo-Kandy", "Colombo", "Kandy");
        when(routeRepository.findAll()).thenReturn(List.of(kandyRoute));

        List<SearchMatch> matches = service.searchEntity("trip to Kandy");

        assertEquals(1, matches.size());
        assertEquals("route", matches.get(0).type());
        assertEquals("Colombo-Kandy", matches.get(0).title());
        assertEquals(1L, matches.get(0).id());
    }

    @Test
    void searchEntity_noMatchWhenNoRouteMatches() {
        when(routeRepository.findAll()).thenReturn(List.of());

        List<SearchMatch> first = service.searchEntity("trip to Ella");
        List<SearchMatch> second = service.searchEntity("trip to Ella");

        assertEquals(first, second);
        assertTrue(first.isEmpty());
    }

    @Test
    void searchEntity_blankOrNullYieldsEmpty() {
        assertEquals(List.of(), service.searchEntity("what is the refund policy?"));
        assertEquals(List.of(), service.searchEntity("  "));
        assertEquals(List.of(), service.searchEntity(null));
    }

    private TrainRoute route(long id, String name, String origin, String destination) {
        return new TrainRoute("R" + id, name, origin, destination,
                new BigDecimal("1000.00"), 100) {
            @Override
            public Long getId() {
                return id;
            }
        };
    }

    private ProviderProduct product(long id, String title, String origin, String destination) {
        return product(id, title, origin, destination, true);
    }

    private ProviderProduct product(long id, String title, String origin, String destination,
                                    boolean enabled) {
        Tenant tenant = new Tenant("acme", "Acme Travel", "LK", "LKR", "en",
                "Asia/Colombo", "acme.example");
        Provider provider = new Provider("ACME", "Acme Travel", null, tenant, "LK", "LKR",
                "Asia/Colombo", null, "NONE", Provider.ProviderVertical.TRAIN, "TICKET");
        return new ProviderProduct(provider, tenant, ProviderProduct.ProductType.TICKET,
                title, origin, destination, LocalDateTime.now(), new BigDecimal("1500.00"),
                "LKR", 50, null, null) {
            @Override
            public Long getId() {
                return id;
            }

            @Override
            public boolean isEnabled() {
                return enabled;
            }
        };
    }
}