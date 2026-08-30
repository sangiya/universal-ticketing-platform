package com.ticketmesh.service;

import com.ticketmesh.dto.TripLeg;
import com.ticketmesh.dto.TripPlan;
import com.ticketmesh.model.TrainSchedule;
import com.ticketmesh.repository.TrainScheduleRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Automated trip planner: assembles an optimised door-to-door itinerary from the
 * services operating on a given date. Legs are chained so a connection only
 * departs after the previous arrival, and the cheapest feasible chain within
 * the requested leg budget is returned. Fully deterministic.
 */
@Service
public class TripPlannerService {

    private static final int MAX_LEGS = 5;

    private record Candidate(String from, String to, LocalTime departure, LocalTime arrival,
                             BigDecimal fare) {
    }

    private final TrainScheduleRepository scheduleRepository;

    public TripPlannerService(TrainScheduleRepository scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
    }

    public TripPlan plan(Long tenantId, String origin, String destination, int legs,
                         String startDate) {
        String from = normalize(origin);
        String to = normalize(destination);
        int maxLegs = legs <= 0 ? 2 : Math.min(legs, MAX_LEGS);
        if (from == null || to == null) {
            return infeasible(from, to, maxLegs, "Origin and destination are required.");
        }
        if (from.equalsIgnoreCase(to)) {
            return infeasible(from, to, maxLegs, "Origin and destination must differ.");
        }

        LocalDate date;
        try {
            date = startDate == null || startDate.isBlank()
                    ? LocalDate.now()
                    : LocalDate.parse(startDate);
        } catch (RuntimeException ex) {
            return infeasible(from, to, maxLegs,
                    "Invalid startDate; expected an ISO date (yyyy-MM-dd).");
        }

        List<Candidate> candidates = loadCandidates(date);
        if (!hasServiceAt(candidates, from) || !hasServiceAt(candidates, to)) {
            String note = "No services operate on " + date + " from " + from
                    + (hasServiceAt(candidates, to) ? "" : " or to " + to)
                    + ".";
            return infeasible(from, to, maxLegs, note);
        }

        List<Candidate> best = bestChain(candidates, from, to, maxLegs);
        if (best.isEmpty()) {
            return infeasible(from, to, maxLegs,
                    "No combination of " + maxLegs + " or fewer legs connects " + from
                            + " to " + to + " on " + date + ".");
        }

        List<TripLeg> legsList = best.stream()
                .map(c -> new TripLeg(c.from(), c.to(), c.departure(), c.arrival(), c.fare()))
                .toList();
        BigDecimal totalFare = best.stream()
                .map(Candidate::fare)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        String note = "Direct door-to-door itinerary assembled for " + date;
        if (legsList.size() > 1) {
            note += " as a " + legsList.size() + "-leg connection with transfers";
        }
        return new TripPlan(from, to, legsList.size(), legsList, totalFare, true, note);
    }

    private List<Candidate> loadCandidates(LocalDate date) {
        return scheduleRepository.findByServiceDate(date).stream()
                .filter(s -> s.getRoute() != null
                        && s.getRoute().getOrigin() != null
                        && s.getRoute().getDestination() != null
                        && s.getDepartureTime() != null
                        && s.getArrivalTime() != null
                        && s.getFare() != null
                        && s.getAvailableSeats() > 0)
                .map(s -> new Candidate(
                        s.getRoute().getOrigin().trim(),
                        s.getRoute().getDestination().trim(),
                        s.getDepartureTime(), s.getArrivalTime(), s.getFare()))
                .sorted(Comparator.comparing(Candidate::departure)
                        .thenComparing(Candidate::fare))
                .toList();
    }

    private boolean hasServiceAt(List<Candidate> candidates, String station) {
        for (Candidate c : candidates) {
            if (c.from().equalsIgnoreCase(station) || c.to().equalsIgnoreCase(station)) {
                return true;
            }
        }
        return false;
    }

    private List<Candidate> bestChain(List<Candidate> candidates, String from, String to,
                                      int maxLegs) {
        Map<String, List<Candidate>> adjacency = new HashMap<>();
        for (Candidate c : candidates) {
            adjacency.computeIfAbsent(c.from().toLowerCase(), k -> new ArrayList<>()).add(c);
        }
        List<Candidate> best = new ArrayList<>();
        chainDfs(adjacency, from.toLowerCase(), to.toLowerCase(), maxLegs,
                new ArrayList<>(), best);
        return best;
    }

    private void chainDfs(Map<String, List<Candidate>> adjacency, String current, String to,
                          int remaining, List<Candidate> path, List<Candidate> best) {
        if (remaining <= 0) {
            return;
        }
        List<Candidate> departing = adjacency.getOrDefault(current, List.of());
        for (Candidate c : departing) {
            if (revisitsStation(path, c.to())) {
                continue;
            }
            if (!path.isEmpty()) {
                Candidate previous = path.get(path.size() - 1);
                if (c.departure().isBefore(previous.arrival())) {
                    continue;
                }
            }
            path.add(c);
            if (c.to().equalsIgnoreCase(to)) {
                if (better(path, best)) {
                    best.clear();
                    best.addAll(path);
                }
            } else {
                chainDfs(adjacency, c.to().toLowerCase(), to, remaining - 1, path, best);
            }
            path.remove(path.size() - 1);
        }
    }

    private boolean revisitsStation(List<Candidate> path, String station) {
        for (Candidate c : path) {
            if (c.from().equalsIgnoreCase(station)) {
                return true;
            }
        }
        return false;
    }

    private boolean better(List<Candidate> path, List<Candidate> best) {
        if (best.isEmpty()) {
            return true;
        }
        int fareComparison = totalFare(path).compareTo(totalFare(best));
        if (fareComparison != 0) {
            return fareComparison < 0;
        }
        if (path.size() != best.size()) {
            return path.size() < best.size();
        }
        return path.get(0).departure().isBefore(best.get(0).departure());
    }

    private BigDecimal totalFare(List<Candidate> path) {
        BigDecimal sum = BigDecimal.ZERO;
        for (Candidate c : path) {
            sum = sum.add(c.fare());
        }
        return sum;
    }

    private TripPlan infeasible(String origin, String destination, int legs, String note) {
        return new TripPlan(origin, destination, 0, List.of(),
                BigDecimal.ZERO.setScale(2), false, note);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}