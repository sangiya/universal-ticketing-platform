package com.ticketmesh.ml;

import com.ticketmesh.repository.BookingRepository;
import com.ticketmesh.repository.ProductOrderRepository;
import com.ticketmesh.repository.ProviderProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiDataAnalystServiceTest {

    private MlSuiteService mlSuiteService;
    private BookingRepository bookingRepository;
    private ProviderProductRepository productRepository;
    private ProductOrderRepository orderRepository;
    private AiDataAnalystService service;

    @BeforeEach
    void setUp() {
        mlSuiteService = mock(MlSuiteService.class);
        bookingRepository = mock(BookingRepository.class);
        productRepository = mock(ProviderProductRepository.class);
        orderRepository = mock(ProductOrderRepository.class);
        service = new AiDataAnalystService(mlSuiteService, bookingRepository,
                productRepository, orderRepository);
    }

    @Test
    void answer_revenueQuestionReturnsStructuredNumbers() {
        TrendReport report = new TrendReport(7L, 3, new BigDecimal("3900.00"),
                new BigDecimal("1300.00"),
                List.of(new DomainStat("TICKET", 2, new BigDecimal("3000.00"))));
        when(mlSuiteService.trendReport(7L)).thenReturn(report);

        AnalystAnswer result = service.answer("What is our total revenue?", 7L);

        assertEquals("What is our total revenue?", result.question());
        assertTrue(result.answer().contains("3900.00"));
        assertEquals(new BigDecimal("3900.00"), result.data().get("totalRevenue"));
        assertEquals(3L, result.data().get("totalOrders"));
        assertEquals("TICKET", result.data().get("topDomain"));
    }

    @Test
    void answer_neverThrowsWhenMetricsUnavailable() {
        when(mlSuiteService.trendReport(anyLong()))
                .thenThrow(new RuntimeException("database unavailable"));

        AnalystAnswer result = service.answer("show me the revenue breakdown", 7L);

        assertNotNull(result);
        assertEquals("show me the revenue breakdown", result.question());
        assertTrue(result.answer().contains("could not summarize"));
        assertEquals("unavailable", result.data().get("state"));
    }

    @Test
    void answer_blankQuestionFallsBackToOverallSummary() {
        TrendReport report = new TrendReport(7L, 2, new BigDecimal("1200.00"),
                new BigDecimal("600.00"), List.of());
        when(mlSuiteService.trendReport(7L)).thenReturn(report);
        when(bookingRepository.count()).thenReturn(4L);
        when(orderRepository.findAll()).thenReturn(List.of());
        when(productRepository.findByTenant_IdAndEnabledTrue(7L)).thenReturn(List.of());

        AnalystAnswer result = service.answer("  ", 7L);

        assertTrue(result.answer().contains("summary"));
        assertEquals(2L, result.data().get("totalOrders"));
        assertEquals(4L, result.data().get("bookings"));
        assertEquals(new BigDecimal("1200.00"), result.data().get("totalRevenue"));
        assertTrue(result.data().get("averageAnomalyScore") instanceof Number);
        assertTrue(((Number) result.data().get("averageAnomalyScore")).doubleValue() >= 0.0);
    }

    @Test
    void answer_fraudQuestionInvokesRiskSignals() {
        when(orderRepository.findAll()).thenReturn(List.of());

        AnalystAnswer result = service.answer("Do we have any fraud signals today?", 7L);

        assertTrue(result.answer().contains("suspicious"));
        assertTrue(result.data().containsKey("flaggedOrders"));
        assertTrue(result.data().containsKey("maxAnomalyScore"));
        assertEquals(0.0, ((Number) result.data().get("maxAnomalyScore")).doubleValue());
    }

    @Test
    void answer_popularProductsQuestionIsDeterministic() {
        when(productRepository.findByTenant_IdAndEnabledTrue(7L)).thenReturn(List.of());
        when(orderRepository.findAll()).thenReturn(List.of());

        AnalystAnswer first = service.answer("what products are popular", 7L);
        AnalystAnswer second = service.answer("what products are popular", 7L);

        assertEquals(first.answer(), second.answer());
        assertEquals(((List<?>) first.data().get("topProducts")).size(),
                ((List<?>) second.data().get("topProducts")).size());
        assertTrue(first.data() instanceof Map);
    }
}