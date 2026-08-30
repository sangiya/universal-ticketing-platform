package com.ticketmesh.ml;

import com.ticketmesh.model.ProductOrder;
import com.ticketmesh.model.ProviderProduct;
import com.ticketmesh.repository.BookingRepository;
import com.ticketmesh.repository.ProductOrderRepository;
import com.ticketmesh.repository.ProviderProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiDataAnalystService {

    private static final Logger log = LoggerFactory.getLogger(AiDataAnalystService.class);

    private final MlSuiteService mlSuiteService;
    private final BookingRepository bookingRepository;
    private final ProviderProductRepository productRepository;
    private final ProductOrderRepository orderRepository;

    public AiDataAnalystService(MlSuiteService mlSuiteService,
                                BookingRepository bookingRepository,
                                ProviderProductRepository productRepository,
                                ProductOrderRepository orderRepository) {
        this.mlSuiteService = mlSuiteService;
        this.bookingRepository = bookingRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    public AnalystAnswer answer(String question, Long tenantId) {
        String raw = question == null ? "" : question;
        String q = raw.toLowerCase();
        try {
            Map<String, Object> data = new LinkedHashMap<>();
            if (containsAny(q, "revenue", "sales", "spend", "income", "money", "earnings")) {
                return revenueAnswer(raw, tenantId, data);
            }
            if (containsAny(q, "booking", "order", "ticket sold")) {
                return ordersAnswer(raw, tenantId, data);
            }
            if (containsAny(q, "product", "catalog", "top", "popular", "recommend", "best")) {
                return productsAnswer(raw, tenantId, data);
            }
            if (containsAny(q, "fraud", "risk", "anomal", "suspicious", "security")) {
                return riskAnswer(raw, tenantId, data);
            }
            return summaryAnswer(raw, tenantId, data);
        } catch (RuntimeException ex) {
            log.warn("AiDataAnalystService failed to answer for tenant {}", tenantId, ex);
            return new AnalystAnswer(raw,
                    "I could not summarize the tenant metrics right now.",
                    Map.of("state", "unavailable"));
        }
    }

    private AnalystAnswer revenueAnswer(String question, Long tenantId, Map<String, Object> data) {
        TrendReport trend = mlSuiteService.trendReport(tenantId);
        String topDomain = trend.domains().isEmpty() ? null : trend.domains().get(0).productType();
        data.put("tenantId", tenantId);
        data.put("totalRevenue", trend.totalRevenue());
        data.put("totalOrders", trend.totalOrders());
        data.put("averageOrderValue", trend.averageOrderValue());
        data.put("topDomain", topDomain);

        String answer = String.format(
                "Revenue for tenant %s totals %s across %d orders, averaging %s per order.",
                tenantId, trend.totalRevenue(), trend.totalOrders(), trend.averageOrderValue());
        if (topDomain != null) {
            answer += " The strongest domain is " + topDomain + ".";
        }
        return new AnalystAnswer(question, answer, data);
    }

    private AnalystAnswer ordersAnswer(String question, Long tenantId, Map<String, Object> data) {
        TrendReport trend = mlSuiteService.trendReport(tenantId);
        long bookings = bookingRepository.count();
        data.put("tenantId", tenantId);
        data.put("totalOrders", trend.totalOrders());
        data.put("totalRevenue", trend.totalRevenue());
        data.put("bookings", bookings);
        List<Map<String, Object>> domains = new ArrayList<>();
        for (DomainStat stat : trend.domains()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("productType", stat.productType());
            entry.put("orderCount", stat.orderCount());
            entry.put("revenue", stat.revenue());
            domains.add(entry);
        }
        data.put("domains", domains);

        String answer = String.format(
                "Tenant %s has %d marketplace orders and %d bookings. "
                        + "Breakdown by product type: %s.",
                tenantId, trend.totalOrders(), bookings, describeDomains(trend.domains()));
        return new AnalystAnswer(question, answer, data);
    }

    private AnalystAnswer productsAnswer(String question, Long tenantId, Map<String, Object> data) {
        List<ProviderProduct> enabled = productRepository.findByTenant_IdAndEnabledTrue(tenantId);
        Map<Long, Integer> popularity = new LinkedHashMap<>();
        for (ProductOrder order : orderRepository.findAll()) {
            ProviderProduct ordered = order.getProduct();
            if (ordered != null && ordered.getId() != null) {
                popularity.merge(ordered.getId(), order.getQuantity(), Integer::sum);
            }
        }
        List<ProviderProduct> ranked = enabled.stream()
                .sorted(Comparator.comparingInt(p -> -popularity.getOrDefault(p.getId(), 0)))
                .limit(5)
                .toList();

        List<Map<String, Object>> top = new ArrayList<>();
        for (ProviderProduct p : ranked) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("productId", p.getId());
            entry.put("title", p.getTitle());
            entry.put("productType", p.getProductType().name());
            entry.put("orders", popularity.getOrDefault(p.getId(), 0));
            top.add(entry);
        }
        data.put("tenantId", tenantId);
        data.put("topProducts", top);

        StringBuilder names = new StringBuilder();
        for (ProviderProduct p : ranked) {
            if (names.length() > 0) {
                names.append(", ");
            }
            names.append(p.getTitle());
        }
        String answer = ranked.isEmpty()
                ? "There are no enabled products for tenant " + tenantId + " yet."
                : "The most popular products for tenant " + tenantId + " are: " + names + ".";
        return new AnalystAnswer(question, answer, data);
    }

    private AnalystAnswer riskAnswer(String question, Long tenantId, Map<String, Object> data) {
        List<ProductOrder> orders = new ArrayList<>(orderRepository.findAll());
        orders.sort(Comparator.comparing(ProductOrder::getTotalAmount,
                Comparator.nullsFirst(BigDecimal::compareTo)).reversed());

        List<Map<String, Object>> flagged = new ArrayList<>();
        double maxScore = 0.0;
        for (int i = 0; i < Math.min(3, orders.size()); i++) {
            ProductOrder order = orders.get(i);
            BigDecimal amount = order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount();
            double score = mlSuiteService.anomalyScore(amount);
            maxScore = Math.max(maxScore, score);
            if (score >= 50) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("orderRef", order.getOrderRef());
                entry.put("amount", amount);
                entry.put("anomalyScore", score);
                flagged.add(entry);
            }
        }
        data.put("tenantId", tenantId);
        data.put("flaggedOrders", flagged);
        data.put("maxAnomalyScore", round2(maxScore));

        String answer = flagged.isEmpty()
                ? "No unusually large or suspicious order amounts were detected for tenant "
                        + tenantId + "."
                : "Found " + flagged.size() + " order(s) with elevated anomaly scores; "
                        + "review before settlement confirmation.";
        return new AnalystAnswer(question, answer, data);
    }

    private AnalystAnswer summaryAnswer(String question, Long tenantId, Map<String, Object> data) {
        TrendReport trend = mlSuiteService.trendReport(tenantId);
        long bookings = bookingRepository.count();
        double avgAnomaly = 0.0;
        List<ProductOrder> orders = orderRepository.findAll();
        if (!orders.isEmpty()) {
            double sum = orders.stream()
                    .map(ProductOrder::getTotalAmount)
                    .filter(amount -> amount != null)
                    .mapToDouble(mlSuiteService::anomalyScore)
                    .sum();
            avgAnomaly = sum / orders.size();
        }
        data.put("tenantId", tenantId);
        data.put("totalOrders", trend.totalOrders());
        data.put("totalRevenue", trend.totalRevenue());
        data.put("averageOrderValue", trend.averageOrderValue());
        data.put("bookings", bookings);
        data.put("averageAnomalyScore", round2(avgAnomaly));

        String answer = String.format(
                "Overall summary for tenant %s: %d marketplace orders, %d bookings "
                        + "and %s total revenue.",
                tenantId, trend.totalOrders(), bookings, trend.totalRevenue());
        return new AnalystAnswer(question, answer, data);
    }

    private String describeDomains(List<DomainStat> domains) {
        StringBuilder sb = new StringBuilder();
        for (DomainStat stat : domains) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(stat.productType()).append("=").append(stat.orderCount())
                    .append(" (").append(stat.revenue()).append(")");
        }
        return sb.length() == 0 ? "no orders yet" : sb.toString();
    }

    private boolean containsAny(String value, String... terms) {
        for (String term : terms) {
            if (value.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private double round2(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}