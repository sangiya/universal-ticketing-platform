package com.ticketmesh.ai.service;

import com.ticketmesh.ai.dto.SearchMatch;
import com.ticketmesh.dto.UniversalOffer;
import com.ticketmesh.dto.UniversalSearchRequest;
import com.ticketmesh.model.ProviderProduct.ProductType;
import com.ticketmesh.model.TrainRoute;
import com.ticketmesh.repository.TrainRouteRepository;
import com.ticketmesh.service.UniversalSearchService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rule-based conversational intent detection and entity search. Fully
 * deterministic (no model invocation) so it can route free-text questions to
 * the right tool surface without external latency.
 *
 * <p>Spec §14: parses natural-language intent into structured search fields,
 * echoes the parsed chips back to the caller, and routes to the universal
 * search service.
 */
@Service
public class IntentService {

    private static final Set<String> STOP_WORDS = Set.of(
            "the", "and", "for", "you", "your", "with", "what", "how", "when",
            "where", "which", "that", "this", "have", "want", "need", "please",
            "show", "tell", "me", "about", "from", "to", "is", "are", "can",
            "do", "does", "i", "i'd", "i'm", "looking", "find", "would",
            "like", "get", "some", "any");

    private static final Map<String, String> VERTICAL_KEYWORDS = new LinkedHashMap<>();
    static {
        VERTICAL_KEYWORDS.put("BUS", "bus");
        VERTICAL_KEYWORDS.put("TRAIN", "train|rail|metro|subway");
        VERTICAL_KEYWORDS.put("FLIGHT", "flight|airline|plane|airport|fly");
        VERTICAL_KEYWORDS.put("MOVIE", "movie|cinema|film|theater|theatre|showtime");
        VERTICAL_KEYWORDS.put("EVENT", "event|concert|show|festival|performance");
        VERTICAL_KEYWORDS.put("SPORTS", "sport|match|game|tournament|cricket|football");
        VERTICAL_KEYWORDS.put("FERRY", "ferry|boat|cruise|sail");
        VERTICAL_KEYWORDS.put("ATTRACTION", "attraction|museum|park|tour|zoo|theme");
    }

    private static final Pattern DATE_PATTERN = Pattern.compile(
            "\\b(\\d{4}-\\d{2}-\\d{2}|\\d{1,2}/\\d{1,2}/\\d{2,4}|tomorrow|today|tonight|next\\s+\\w+)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PASSENGER_PATTERN = Pattern.compile(
            "\\b(\\d+)\\s*(passengers?|tickets?|people|persons?|adults?|pax)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PRICE_PATTERN = Pattern.compile(
            "under\\s+([\\d,.]+)\\s*([a-z]{3})?",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern FROM_TO_PATTERN = Pattern.compile(
            "(?:from\\s+)?([a-z][a-z\\s'-]{2,40}?)\\s+(?:to|->|→)\\s+([a-z][a-z\\s'-]{2,40}?)",
            Pattern.CASE_INSENSITIVE);

    private final TrainRouteRepository routeRepository;
    private final UniversalSearchService universalSearchService;

    public IntentService(TrainRouteRepository routeRepository,
                        UniversalSearchService universalSearchService) {
        this.routeRepository = routeRepository;
        this.universalSearchService = universalSearchService;
    }

    public Intent detectIntent(String question) {
        if (question == null || question.isBlank()) {
            return Intent.GENERAL;
        }
        String q = question.toLowerCase();
        if (containsAny(q, "book", "reserve", "booking", "reservation", "purchase", "buy")) {
            return Intent.BOOKING_HELP;
        }
        if (containsAny(q, "price", "cost", "fare", "how much", "cheap", "expensive", "rate")) {
            return Intent.PRICE_CHECK;
        }
        if (containsAny(q, "support", "refund", "cancel", "cancelled", "complaint", "problem",
                "lost", "damaged", "assist", "contact")) {
            return Intent.SUPPORT;
        }
        if (containsAny(q, "route", "schedule", "train", "timetable", "platform", "station",
                "depart", "arrive", "travel", "trip", "traveling", "ticket", "movie", "concert",
                "flight", "bus", "ferry", "book")) {
            return Intent.SEARCH_ROUTES;
        }
        return Intent.GENERAL;
    }

    /**
     * Parsed free-text fields (spec §14). Each chip is non-null only when the
     * parser found a value.
     */
    public record ParsedIntent(
            String origin,
            String destination,
            LocalDate date,
            Integer passengers,
            BigDecimal maxPrice,
            String currency,
            ProductType productType,
            String rawQuery) {}

    /**
     * Parse the natural-language query into a typed search request. Used by the
     * conversational search surface.
     */
    public ParsedIntent parse(String question) {
        if (question == null || question.isBlank()) {
            return new ParsedIntent(null, null, null, null, null, null, null, "");
        }
        String lower = question.toLowerCase();
        OriginDest od = extractFromTo(question);
        LocalDate date = extractDate(question);
        Integer passengers = extractPassengers(lower);
        BigDecimal maxPrice = extractMaxPrice(lower);
        ProductType productType = extractVertical(lower);
        return new ParsedIntent(
                od != null ? od.origin : null,
                od != null ? od.destination : null,
                date,
                passengers,
                maxPrice,
                "LKR",
                productType,
                question.trim());
    }

    /**
     * Run a structured search using parsed intent. Returns up to 5 matches.
     */
    public List<UniversalOffer> searchWithParsedIntent(String question, Long tenantId, int limit) {
        ParsedIntent parsed = parse(question);
        UniversalSearchRequest req = new UniversalSearchRequest(
                parsed.rawQuery(),
                parsed.origin(),
                parsed.destination(),
                parsed.date(),
                parsed.date(),
                parsed.passengers(),
                null,
                parsed.maxPrice(),
                parsed.productType(),
                null,
                "price", "asc",
                limit);
        return universalSearchService.search(req, tenantId);
    }

    public List<SearchMatch> searchEntity(String question) {
        if (question == null || question.isBlank()) {
            return List.of();
        }
        String lower = question.toLowerCase();
        List<SearchMatch> matches = new ArrayList<>();
        for (TrainRoute route : routeRepository.findAll()) {
            if (route.getId() == null) continue;
            if (matchesText(lower, route.getName()) || matchesText(lower, route.getOrigin())
                    || matchesText(lower, route.getDestination()) || matchesText(lower, route.getCode())) {
                matches.add(new SearchMatch("route", route.getName(), route.getId()));
            }
        }
        return matches;
    }

    // --- Parsing helpers ---

    private record OriginDest(String origin, String destination) {}

    private OriginDest extractFromTo(String text) {
        if (text == null) return null;
        Matcher m = FROM_TO_PATTERN.matcher(text);
        if (m.find()) {
            String o = clean(m.group(1));
            String d = clean(m.group(2));
            if (o != null && d != null && o.length() >= 2 && d.length() >= 2) {
                return new OriginDest(o, d);
            }
        }
        return null;
    }

    private String clean(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim()
                .replaceAll("^\\W+|\\W+$", "")
                .trim();
        if (trimmed.isBlank()) return null;
        return Character.toUpperCase(trimmed.charAt(0)) + trimmed.substring(1);
    }

    private LocalDate extractDate(String text) {
        if (text == null) return null;
        String lower = text.toLowerCase();
        if (lower.contains("tomorrow")) {
            return LocalDate.now().plusDays(1);
        }
        if (lower.contains("tonight") || lower.contains("today")) {
            return LocalDate.now();
        }
        Matcher m = DATE_PATTERN.matcher(text);
        if (m.find()) {
            String token = m.group(1);
            try {
                if (token.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    return LocalDate.parse(token);
                }
                if (token.matches("\\d{1,2}/\\d{1,2}/\\d{2,4}")) {
                    String[] parts = token.split("/");
                    int day = Integer.parseInt(parts[0]);
                    int month = Integer.parseInt(parts[1]);
                    int year = Integer.parseInt(parts[2]);
                    if (year < 100) year += 2000;
                    return LocalDate.of(year, month, day);
                }
            } catch (DateTimeParseException | NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }

    private Integer extractPassengers(String lower) {
        Matcher m = PASSENGER_PATTERN.matcher(lower);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }

    private BigDecimal extractMaxPrice(String lower) {
        Matcher m = PRICE_PATTERN.matcher(lower);
        if (m.find()) {
            try {
                String amount = m.group(1).replaceAll(",", "");
                return new BigDecimal(amount);
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }

    private ProductType extractVertical(String lower) {
        for (Map.Entry<String, String> entry : VERTICAL_KEYWORDS.entrySet()) {
            if (matchesAny(lower, entry.getValue().split("\\|"))) {
                return switch (entry.getKey()) {
                    case "BUS", "TRAIN", "FLIGHT", "FERRY" -> ProductType.ROUTE;
                    case "MOVIE", "EVENT", "SPORTS", "ATTRACTION" -> ProductType.ADMISSION;
                    default -> null;
                };
            }
        }
        return null;
    }

    private boolean matchesAny(String value, String... terms) {
        for (String term : terms) {
            if (value.contains(term)) return true;
        }
        return false;
    }

    private boolean matchesText(String lowerQuestion, String target) {
        if (target == null || target.isBlank()) {
            return false;
        }
        String lowerTarget = target.toLowerCase();
        String[] tokens = lowerQuestion.replaceAll("[^a-z0-9 ]", " ").split("\\s+");
        for (String token : tokens) {
            if (token.length() >= 3 && !STOP_WORDS.contains(token)
                    && lowerTarget.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsAny(String value, String... terms) {
        for (String term : terms) {
            if (value.contains(term)) {
                return true;
            }
        }
        return false;
    }
}
