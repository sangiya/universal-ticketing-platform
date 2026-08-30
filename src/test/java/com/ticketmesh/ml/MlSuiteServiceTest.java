package com.ticketmesh.ml;

import com.ticketmesh.model.Booking;
import com.ticketmesh.model.ProductOrder;
import com.ticketmesh.model.Provider;
import com.ticketmesh.model.ProviderProduct;
import com.ticketmesh.model.Tenant;
import com.ticketmesh.model.TrainRoute;
import com.ticketmesh.model.TrainSchedule;
import com.ticketmesh.model.User;
import com.ticketmesh.repository.BookingRepository;
import com.ticketmesh.repository.ProductOrderRepository;
import com.ticketmesh.repository.ProviderProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MlSuiteServiceTest {

    private BookingRepository bookingRepository;
    private ProviderProductRepository productRepository;
    private ProductOrderRepository orderRepository;
    private MlSuiteService service;

    private Tenant tenant;
    private Provider provider;

    @BeforeEach
    void setUp() {
        bookingRepository = mock(BookingRepository.class);
        productRepository = mock(ProviderProductRepository.class);
        orderRepository = mock(ProductOrderRepository.class);
        service = new MlSuiteService(bookingRepository, productRepository, orderRepository);

        tenant = new Tenant("acme", "Acme Travel", "LK", "LKR", "en", "Asia/Colombo",
                "acme.example");
        provider = new Provider("ACME", "Acme Travel", null, tenant, "LK", "LKR",
                "Asia/Colombo", null, "NONE", Provider.ProviderVertical.TRAIN, "TICKET");
    }

    @Test
    void demandForecast_projectsPositiveDemandWithBoundedConfidence() {
        ProviderProduct product = product(1L, "Night Express",
                ProviderProduct.ProductType.TICKET, new BigDecimal("1500.00"), 50);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(orderRepository.findAll()).thenReturn(List.of(
                order("ticketmesh-ORD1", product, Instant.now().minus(Duration.ofDays(1))),
                order("ticketmesh-ORD2", product, Instant.now().minus(Duration.ofDays(2))),
                order("ticketmesh-ORD3", product, Instant.now().minus(Duration.ofDays(3)))));

        DemandForecast forecast = service.demandForecast(1L, 7);

        assertEquals(7, forecast.projections().size());
        assertEquals(1, forecast.projections().get(0).dayOffset());
        assertEquals(7, forecast.projections().get(6).dayOffset());
        double totalDemand = forecast.projections().stream()
                .mapToDouble(DayProjection::projectedDemand)
                .sum();
        assertTrue(totalDemand > 0.0, "projected demand must be positive");
        assertTrue(forecast.projections().stream()
                .allMatch(p -> p.projectedDemand() >= 0.0));
        assertTrue(forecast.confidence() > 0.30 && forecast.confidence() <= 1.0);
    }

    @Test
    void cancellationProbability_staysInUnitIntervalAndFlagsRiskyBookings() {
        User alice = new User("alice", "pw", "Alice", "alice@example.com", User.Role.CUSTOMER) {
            @Override
            public Long getId() {
                return 1L;
            }
        };
        TrainRoute route = new TrainRoute("COL-KAN", "Colombo-Kandy", "Colombo", "Kandy",
                new BigDecimal("1200.00"), 115);
        TrainSchedule schedule = new TrainSchedule(route, "EX-1001", LocalDate.now().plusDays(2),
                LocalTime.of(8, 30), LocalTime.of(12, 15), 60, new BigDecimal("1200.00"));

        Booking risky = new Booking("ticketmesh-R1", alice, schedule, LocalDate.now().plusDays(1),
                "Alice", 1, new BigDecimal("6000.00"), Booking.Status.RESERVED);
        Booking safe = new Booking("ticketmesh-S1", alice, schedule, LocalDate.now().plusDays(20),
                "Alice", 2, new BigDecimal("800.00"), Booking.Status.PAID);
        Booking cancelled = new Booking("ticketmesh-C1", alice, schedule,
                LocalDate.now().minusDays(5), "Alice", 3, new BigDecimal("1200.00"),
                Booking.Status.CANCELLED);

        when(bookingRepository.findByUserIdOrderByCreatedAtDesc(anyLong()))
                .thenReturn(List.of(cancelled));

        double riskyProbability = service.cancellationProbability(risky);
        double safeProbability = service.cancellationProbability(safe);

        assertTrue(riskyProbability >= 0.0 && riskyProbability <= 1.0);
        assertTrue(safeProbability >= 0.0 && safeProbability <= 1.0);
        assertTrue(riskyProbability > safeProbability);
        assertTrue(riskyProbability > 0.5);
        assertTrue(safeProbability < 0.5);
        assertEquals(0.0, service.cancellationProbability(null), 1e-9);
    }

    @Test
    void anomalyScore_ranksLargerAmountsHigherWithinBounds() {
        double small = service.anomalyScore(new BigDecimal("10"));
        double medium = service.anomalyScore(new BigDecimal("1000"));
        double large = service.anomalyScore(new BigDecimal("100000"));

        assertTrue(small >= 0.0 && small <= 100.0);
        assertTrue(medium >= 0.0 && medium <= 100.0);
        assertTrue(large >= 0.0 && large <= 100.0);
        assertTrue(small < medium);
        assertTrue(medium < large);
        assertEquals(0.0, service.anomalyScore(BigDecimal.ZERO), 1e-9);
        assertEquals(0.0, service.anomalyScore(new BigDecimal("-5")), 1e-9);
        assertEquals(0.0, service.anomalyScore(null), 1e-9);
    }

    @Test
    void recommend_returnsMostPopularProductsWhenUserHasNoHistory() {
        long tenantId = 7L;
        ProviderProduct topProduct = product(1L, "Grand Finale", ProviderProduct.ProductType.ADMISSION,
                new BigDecimal("2000.00"), 20);
        ProviderProduct quietProduct = product(2L, "Economy Coach", ProviderProduct.ProductType.SEAT,
                new BigDecimal("700.00"), 60);
        when(productRepository.findByTenant_IdAndEnabledTrue(tenantId))
                .thenReturn(List.of(topProduct, quietProduct));
        when(orderRepository.findAll()).thenReturn(List.of(
                order("ticketmesh-A1", topProduct, Instant.now().minus(Duration.ofDays(2))),
                order("ticketmesh-A2", topProduct, Instant.now().minus(Duration.ofDays(1))),
                order("ticketmesh-A3", topProduct, Instant.now())));
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(9L)).thenReturn(List.of());

        List<Recommendation> recommendations = service.recommend(9L, tenantId, 5);

        assertEquals(2, recommendations.size());
        assertEquals(1L, recommendations.get(0).productId());
        assertTrue(recommendations.get(0).score() > recommendations.get(1).score());
    }

    @Test
    void recommend_prefersProductTypesFromUserHistory() {
        long tenantId = 7L;
        ProviderProduct admission = product(1L, "Grand Finale", ProviderProduct.ProductType.ADMISSION,
                new BigDecimal("2000.00"), 20);
        ProviderProduct seat = product(2L, "Economy Coach", ProviderProduct.ProductType.SEAT,
                new BigDecimal("700.00"), 60);
        when(productRepository.findByTenant_IdAndEnabledTrue(tenantId))
                .thenReturn(List.of(admission, seat));

        ProductOrder historyOrder = order("ticketmesh-H1", seat, Instant.now().minus(Duration.ofDays(1)));
        when(orderRepository.findAll()).thenReturn(List.of(historyOrder));
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(9L)).thenReturn(List.of(historyOrder));

        List<Recommendation> recommendations = service.recommend(9L, tenantId, 5);

        assertEquals(2, recommendations.size());
        assertEquals(2L, recommendations.get(0).productId());
        assertEquals(1L, recommendations.get(1).productId());
        assertTrue(recommendations.get(0).score() > recommendations.get(1).score());
        assertTrue(recommendations.get(0).reason().contains("seat purchases"));
    }

    @Test
    void predictPrice_compoundsGrowthAboveBasePrice() {
        ProviderProduct scarce = product(1L, "Festival Front Seat", ProviderProduct.ProductType.ADMISSION,
                new BigDecimal("1000.00"), 3);
        when(productRepository.findById(1L)).thenReturn(Optional.of(scarce));

        PricePrediction prediction = service.predictPrice(1L, 5);

        assertEquals(5, prediction.projections().size());
        assertTrue(prediction.growthRate() > 0.0);
        assertEquals(new BigDecimal("1000.00"), prediction.basePrice());
        BigDecimal previous = prediction.basePrice();
        for (PriceProjection projection : prediction.projections()) {
            assertTrue(projection.predictedPrice().compareTo(previous) >= 0);
            assertTrue(projection.predictedPrice().compareTo(prediction.basePrice()) >= 0);
            previous = projection.predictedPrice();
        }
        assertTrue(prediction.projections().get(4).predictedPrice()
                .compareTo(prediction.basePrice()) > 0);
    }

    @Test
    void trendReport_aggregatesOrdersByProductTypeAndTotals() {
        ProviderProduct ticket = product(1L, "Ticket", ProviderProduct.ProductType.TICKET,
                new BigDecimal("1500.00"), 50);
        ProviderProduct seat = product(2L, "Seat", ProviderProduct.ProductType.SEAT,
                new BigDecimal("900.00"), 10);
        when(orderRepository.findByTenant_IdOrderByCreatedAtDesc(7L)).thenReturn(List.of(
                order("ticketmesh-T1", ticket, Instant.now(), new BigDecimal("1500.00")),
                order("ticketmesh-T2", ticket, Instant.now(), new BigDecimal("1500.00")),
                order("ticketmesh-S1", seat, Instant.now(), new BigDecimal("900.00"))));

        TrendReport report = service.trendReport(7L);

        assertEquals(3, report.totalOrders());
        assertEquals(new BigDecimal("3900.00"), report.totalRevenue());
        assertEquals(new BigDecimal("1300.00"), report.averageOrderValue());
        assertEquals(2, report.domains().size());
        assertEquals("TICKET", report.domains().get(0).productType());
        assertEquals(2, report.domains().get(0).orderCount());
        assertEquals(new BigDecimal("3000.00"), report.domains().get(0).revenue());
    }

    private ProviderProduct product(long id, String title, ProviderProduct.ProductType type,
                                    BigDecimal price, int quantity) {
        return new ProviderProduct(provider, tenant, type, title, null, null, null,
                price, "LKR", quantity, null, null) {
            @Override
            public Long getId() {
                return id;
            }
        };
    }

    private ProductOrder order(String ref, ProviderProduct product, Instant createdAt) {
        return order(ref, product, createdAt, product.getPrice());
    }

    private ProductOrder order(String ref, ProviderProduct product, Instant createdAt,
                               BigDecimal total) {
        User user = new User("alice", "pw", "Alice", "alice@example.com", User.Role.CUSTOMER);
        return new ProductOrder(ref, tenant, user, product, 1, product.getPrice(), "LKR",
                total, new BigDecimal("0.00"), new BigDecimal("0.00"), BigDecimal.ZERO,
                total, null) {
            @Override
            public Instant getCreatedAt() {
                return createdAt;
            }
        };
    }
}