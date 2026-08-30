package com.ticketmesh.ai.service;

import com.ticketmesh.ai.dto.SearchMatch;
import com.ticketmesh.model.ProviderProduct;
import com.ticketmesh.model.TrainRoute;
import com.ticketmesh.repository.ProviderProductRepository;
import com.ticketmesh.repository.TrainRouteRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Rule-based conversational intent detection and entity search. Fully
 * deterministic (no model invocation) so it can route free-text questions to
 * the right tool surface without external latency.
 */
@Service
public class IntentService {

    private static final Set<String> STOP_WORDS = Set.of(
            "the", "and", "for", "you", "your", "with", "what", "how", "when",
            "where", "which", "that", "this", "have", "want", "need", "please",
            "show", "tell", "me", "about", "from", "to", "is", "are", "can");

    private final ProviderProductRepository productRepository;
    private final TrainRouteRepository routeRepository;

    public IntentService(ProviderProductRepository productRepository,
                         TrainRouteRepository routeRepository) {
        this.productRepository = productRepository;
        this.routeRepository = routeRepository;
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
                "depart", "arrive", "travel", "trip", "traveling")) {
            return Intent.SEARCH_ROUTES;
        }
        return Intent.GENERAL;
    }

    public List<SearchMatch> searchEntity(String question) {
        if (question == null || question.isBlank()) {
            return List.of();
        }
        String lower = question.toLowerCase();
        List<SearchMatch> matches = new ArrayList<>();
        for (TrainRoute route : routeRepository.findAll()) {
            if (route.getId() == null) {
                continue;
            }
            if (matchesText(lower, route.getName()) || matchesText(lower, route.getOrigin())
                    || matchesText(lower, route.getDestination()) || matchesText(lower, route.getCode())) {
                matches.add(new SearchMatch("route", route.getName(), route.getId()));
            }
        }
        for (ProviderProduct product : productRepository.findAll()) {
            if (!product.isEnabled() || product.getId() == null || product.getTitle() == null) {
                continue;
            }
            if (matchesText(lower, product.getTitle()) || matchesText(lower, product.getOrigin())
                    || matchesText(lower, product.getDestination())) {
                matches.add(new SearchMatch("product", product.getTitle(), product.getId()));
            }
        }
        return matches;
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