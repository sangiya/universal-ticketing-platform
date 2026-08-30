package com.ticketmesh.ml;

import com.ticketmesh.model.Booking;
import com.ticketmesh.model.ProductOrder;
import com.ticketmesh.model.ProviderProduct;
import com.ticketmesh.model.Tenant;
import com.ticketmesh.model.User;
import com.ticketmesh.repository.BookingRepository;
import com.ticketmesh.repository.ProductOrderRepository;
import com.ticketmesh.repository.ProviderProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MlSuiteService {

    private static final Logger log = LoggerFactory.getLogger(MlSuiteService.class);
    private static final int MAX_HORIZON_DAYS = 30;
    private static final int LOOKBACK_DAYS = 30;
    private static final int SEASONAL_WINDOW_DAYS = 90;
    private static final ZoneOffset UTC = ZoneOffset.UTC;

    private final BookingRepository bookingRepository;
    private final ProviderProductRepository productRepository;
    private final ProductOrderRepository orderRepository;
    private final SeatRecommender seatRecommender = new SeatRecommender();

    public MlSuiteService(BookingRepository bookingRepository,
                          ProviderProductRepository productRepository,
                          ProductOrderRepository orderRepository) {
        this.bookingRepository = bookingRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public DemandForecast demandForecast(Long productId, int horizonDays) {
        int horizon = Math.max(1, Math.min(horizonDays, MAX_HORIZON_DAYS));
        if (productId == null || productRepository.findById(productId).isEmpty()) {
            return new DemandForecast(productId, horizon, 0.0, List.of());
        }

        List<ProductOrder> orders = ordersForProduct(productId);
        LocalDate today = LocalDate.now(UTC);

        Map<LocalDate, Integer> dailyCounts = new HashMap<>();
        for (ProductOrder order : orders) {
            Instant created = order.getCreatedAt();
            if (created == null) {
                continue;
            }
            LocalDate day = LocalDate.ofInstant(created, UTC);
            if (!day.isBefore(today.minusDays(LOOKBACK_DAYS - 1)) && !day.isAfter(today)) {
                dailyCounts.merge(day, 1, Integer::sum);
            }
        }

        List<LocalDate> series = new ArrayList<>();
        for (LocalDate day = today.minusDays(LOOKBACK_DAYS - 1); !day.isAfter(today); day = day.plusDays(1)) {
            series.add(day);
        }

        double baseRate = series.stream()
                .mapToDouble(day -> dailyCounts.getOrDefault(day, 0))
                .average().orElse(0.0);
        double slope = linearSlopePerDay(series, dailyCounts);
        Map<DayOfWeek, Double> seasonal = seasonalProfile(orders, today);

        List<DayProjection> projections = new ArrayList<>();
        for (int offset = 1; offset <= horizon; offset++) {
            LocalDate day = today.plusDays(offset);
            double seasonFactor = seasonal.getOrDefault(day.getDayOfWeek(), 1.0);
            double projected = Math.max(0.0, baseRate + slope * offset) * seasonFactor;
            projections.add(new DayProjection(offset, round2(projected), round4(seasonFactor), day.toString()));
        }

        long sampleOrders = series.stream().mapToLong(day -> dailyCounts.getOrDefault(day, 0)).sum();
        double confidence = clamp(0.30 + Math.min(1.0, sampleOrders / 25.0) * 0.70, 0.0, 1.0);

        return new DemandForecast(productId, horizon, confidence, projections);
    }

    public double cancellationProbability(Booking booking) {
        if (booking == null || booking.getStatus() == null) {
            return 0.0;
        }
        double p = 0.05;
        switch (booking.getStatus()) {
            case RESERVED -> p += 0.25;
            case PAID -> p -= 0.05;
            case CANCELLED -> p += 0.45;
            case EXPIRED -> p += 0.35;
        }

        Instant createdAt = booking.getCreatedAt();
        LocalDate travelDate = booking.getTravelDate();
        if (createdAt != null && travelDate != null) {
            LocalDate createdDay = LocalDate.ofInstant(createdAt, UTC);
            long leadDays = ChronoUnit.DAYS.between(createdDay, travelDate);
            if (leadDays < 2) {
                p += 0.20;
            } else if (leadDays < 7) {
                p += 0.10;
            } else {
                p -= 0.05;
            }
        }

        BigDecimal fare = booking.getFare();
        if (fare != null && fare.compareTo(new BigDecimal("5000")) > 0) {
            p += 0.05;
        }

        User user = booking.getUser();
        if (user != null && user.getId() != null) {
            List<Booking> history = bookingRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
            if (!history.isEmpty()) {
                long cancelled = history.stream()
                        .filter(b -> b.getStatus() == Booking.Status.CANCELLED)
                        .count();
                double ratio = cancelled / (double) history.size();
                if (ratio > 0.5) {
                    p += 0.20;
                } else if (ratio > 0.25) {
                    p += 0.10;
                }
            }
        }

        return round2(clamp(p, 0.0, 1.0));
    }

    @Transactional(readOnly = true)
    public List<Recommendation> recommend(Long userId, Long tenantId, int limit) {
        int max = Math.min(Math.max(1, limit <= 0 ? 5 : limit), 20);
        List<ProviderProduct> enabled = productRepository.findByTenant_IdAndEnabledTrue(tenantId);
        if (enabled.isEmpty()) {
            return List.of();
        }

        Set<Long> enabledIds = enabled.stream()
                .map(ProviderProduct::getId)
                .collect(Collectors.toSet());
        Map<Long, Integer> popularity = new HashMap<>();
        for (ProductOrder order : orderRepository.findAll()) {
            ProviderProduct ordered = order.getProduct();
            if (ordered != null && ordered.getId() != null && enabledIds.contains(ordered.getId())) {
                popularity.merge(ordered.getId(), order.getQuantity(), Integer::sum);
            }
        }

        Map<String, Integer> preferences = new LinkedHashMap<>();
        List<ProductOrder> history = userId == null
                ? List.of()
                : orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        for (int i = 0; i < history.size(); i++) {
            ProductOrder order = history.get(i);
            String type = order.getProductType() == null ? "UNKNOWN" : order.getProductType();
            int recencyWeight = Math.max(1, 5 - i / 2);
            preferences.merge(type, recencyWeight, Integer::sum);
        }

        if (preferences.isEmpty()) {
            return enabled.stream()
                    .map(p -> new Recommendation(
                            p.getId(), p.getTitle(), p.getProductType().name(),
                            popularityScore(popularity.getOrDefault(p.getId(), 0)),
                            popularity.getOrDefault(p.getId(), 0) == 0
                                    ? "Available for your tenant"
                                    : "Most popular in your tenant"))
                    .sorted(Comparator.comparing(Recommendation::score).reversed())
                    .limit(max)
                    .toList();
        }

        return enabled.stream()
                .map(p -> scoreRecommendation(p, preferences, popularity))
                .sorted(Comparator.comparing(Recommendation::score).reversed())
                .limit(max)
                .toList();
    }

    @Transactional(readOnly = true)
    public PricePrediction predictPrice(Long productId, int horizonDays) {
        int horizon = Math.max(1, Math.min(horizonDays, MAX_HORIZON_DAYS));
        Optional<ProviderProduct> productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty()) {
            return new PricePrediction(productId, BigDecimal.ZERO.setScale(2), 0.0, horizon, List.of());
        }

        ProviderProduct product = productOpt.get();
        BigDecimal base = product.getPrice() == null ? BigDecimal.ZERO : product.getPrice();
        double growthRate = surgeRate(product);
        BigDecimal projected = base;
        List<PriceProjection> projections = new ArrayList<>();
        LocalDate today = LocalDate.now(UTC);
        for (int offset = 1; offset <= horizon; offset++) {
            projected = projected.multiply(BigDecimal.ONE.add(BigDecimal.valueOf(growthRate)));
            projections.add(new PriceProjection(
                    offset, today.plusDays(offset).toString(),
                    projected.setScale(2, RoundingMode.HALF_UP)));
        }

        return new PricePrediction(productId, base.setScale(2, RoundingMode.HALF_UP),
                growthRate, horizon, projections);
    }

    public double anomalyScore(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            return 0.0;
        }
        double z = (Math.log10(amount.doubleValue()) - 3.0) / 0.6;
        return round1(clamp(50.0 + z * 25.0, 0.0, 100.0));
    }

    public SeatRecommendation recommendSeats(int seatCount, int totalCapacity,
                                             String preference, String takenCsv) {
        return seatRecommender.recommendSeats(
                seatCount, totalCapacity, preference, parseTaken(takenCsv));
    }

    private Set<Integer> parseTaken(String takenCsv) {
        Set<Integer> taken = new HashSet<>();
        if (takenCsv == null || takenCsv.isBlank()) {
            return taken;
        }
        for (String part : takenCsv.split(",")) {
            try {
                taken.add(Integer.parseInt(part.trim()));
            } catch (NumberFormatException ex) {
                // malformed entries are ignored; the seat set stays deterministic
            }
        }
        return taken;
    }

    @Transactional(readOnly = true)
    public TrendReport trendReport(Long tenantId) {
        List<ProductOrder> orders = tenantId == null
                ? List.of()
                : orderRepository.findByTenant_IdOrderByCreatedAtDesc(tenantId);

        Map<String, Long> counts = new LinkedHashMap<>();
        Map<String, BigDecimal> revenue = new LinkedHashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        for (ProductOrder order : orders) {
            String type = order.getProductType() == null ? "UNKNOWN" : order.getProductType();
            counts.merge(type, 1L, Long::sum);
            BigDecimal amount = order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount();
            revenue.merge(type, amount, BigDecimal::add);
            total = total.add(amount);
        }

        List<DomainStat> domains = counts.entrySet().stream()
                .map(e -> new DomainStat(
                        e.getKey(), e.getValue(),
                        revenue.getOrDefault(e.getKey(), BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP)))
                .sorted(Comparator.comparing(DomainStat::revenue).reversed())
                .toList();

        int totalOrders = orders.size();
        BigDecimal average = totalOrders == 0
                ? BigDecimal.ZERO.setScale(2)
                : total.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP);

        return new TrendReport(tenantId, totalOrders,
                total.setScale(2, RoundingMode.HALF_UP), average, domains);
    }

    private List<ProductOrder> ordersForProduct(Long productId) {
        ProviderProduct product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            return List.of();
        }
        Tenant tenant = product.getTenant();
        List<ProductOrder> tenantOrders = tenant != null && tenant.getId() != null
                ? orderRepository.findByTenant_IdOrderByCreatedAtDesc(tenant.getId())
                : orderRepository.findAll();
        List<ProductOrder> matches = new ArrayList<>();
        for (ProductOrder order : tenantOrders) {
            ProviderProduct ordered = order.getProduct();
            if (ordered != null && productId.equals(ordered.getId())) {
                matches.add(order);
            }
        }
        return matches;
    }

    private double linearSlopePerDay(List<LocalDate> days, Map<LocalDate, Integer> counts) {
        int n = days.size();
        if (n < 2) {
            return 0.0;
        }
        double meanX = (n - 1) / 2.0;
        double sumY = 0;
        double sumXY = 0;
        for (int i = 0; i < n; i++) {
            double y = counts.getOrDefault(days.get(i), 0);
            sumY += y;
            sumXY += i * y;
        }
        double meanY = sumY / n;
        double sumSqX = 0;
        for (int i = 0; i < n; i++) {
            sumSqX += (i - meanX) * (i - meanX);
        }
        if (sumSqX == 0) {
            return 0.0;
        }
        return (sumXY - n * meanX * meanY) / sumSqX;
    }

    private Map<DayOfWeek, Double> seasonalProfile(List<ProductOrder> orders, LocalDate today) {
        Map<DayOfWeek, Integer> counts = new EnumMap<>(DayOfWeek.class);
        Map<DayOfWeek, Integer> occurrences = new EnumMap<>(DayOfWeek.class);
        double totalOrders = 0;
        double totalDays = 0;
        for (ProductOrder order : orders) {
            Instant created = order.getCreatedAt();
            if (created == null) {
                continue;
            }
            LocalDate day = LocalDate.ofInstant(created, UTC);
            if (day.isBefore(today.minusDays(SEASONAL_WINDOW_DAYS - 1)) || day.isAfter(today)) {
                continue;
            }
            DayOfWeek dow = day.getDayOfWeek();
            counts.merge(dow, 1, Integer::sum);
            occurrences.merge(dow, 1, Integer::sum);
        }
        for (DayOfWeek dow : DayOfWeek.values()) {
            totalOrders += counts.getOrDefault(dow, 0);
            totalDays += occurrences.getOrDefault(dow, 0);
        }

        Map<DayOfWeek, Double> factors = new EnumMap<>(DayOfWeek.class);
        if (totalDays == 0) {
            for (DayOfWeek dow : DayOfWeek.values()) {
                factors.put(dow, 1.0);
            }
            return factors;
        }
        double globalAvg = totalOrders / totalDays;
        if (globalAvg <= 0) {
            for (DayOfWeek dow : DayOfWeek.values()) {
                factors.put(dow, 1.0);
            }
            return factors;
        }
        for (DayOfWeek dow : DayOfWeek.values()) {
            int occ = occurrences.getOrDefault(dow, 0);
            double avg = occ == 0 ? globalAvg : counts.getOrDefault(dow, 0) / (double) occ;
            factors.put(dow, clamp(avg / globalAvg, 0.2, 3.0));
        }
        return factors;
    }

    private Recommendation scoreRecommendation(ProviderProduct product,
                                               Map<String, Integer> preferences,
                                               Map<Long, Integer> popularity) {
        String type = product.getProductType().name();
        int prefCount = preferences.getOrDefault(type, 0);
        double score = 1.0;
        if (prefCount > 0) {
            score += Math.min(4.0, 1.5 + prefCount * 0.5);
        }
        int pop = popularity.getOrDefault(product.getId(), 0);
        if (pop > 0) {
            score += Math.min(3.0, Math.log1p(pop) * 0.6);
        }
        String reason = prefCount > 0
                ? "Matches your past " + type.toLowerCase() + " purchases"
                : (pop > 0 ? "Popular in your tenant" : "New addition to your tenant");
        return new Recommendation(product.getId(), product.getTitle(), type, round2(score), reason);
    }

    private double popularityScore(int count) {
        return count <= 0 ? 0.0 : round2(Math.min(5.0, 1.0 + Math.log1p(count) * 0.7));
    }

    private double surgeRate(ProviderProduct product) {
        double rate = 0.006;
        if (product.getAvailableQuantity() < 5) {
            rate += 0.045;
        } else if (product.getAvailableQuantity() < 15) {
            rate += 0.02;
        }
        if (product.getEventDate() != null) {
            long daysUntil = ChronoUnit.DAYS.between(
                    LocalDate.now(UTC), product.getEventDate().toLocalDate());
            if (daysUntil >= 0 && daysUntil <= 3) {
                rate += 0.05;
            }
        }
        return round4(rate);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private double round4(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}