package com.ticketmesh.ml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Deterministic seat recommender. Seats are modelled in rows of four
 * (window / aisle / aisle / window). Comfort is scored per seat from its
 * position and the requested preference (WINDOW, AISLE, MIDDLE, FRONT, BACK),
 * taken seats are excluded, and the chosen seats are spread across rows so
 * companions never sit wedged into adjacent spots when better options exist.
 */
public class SeatRecommender {

    private static final int SEATS_PER_ROW = 4;
    private static final String[] PREFERENCES =
            {"WINDOW", "AISLE", "MIDDLE", "FRONT", "BACK"};

    public SeatRecommendation recommendSeats(int seatCount, int totalCapacity,
                                             String preference) {
        return recommendSeats(seatCount, totalCapacity, preference, Set.of());
    }

    public SeatRecommendation recommendSeats(int seatCount, int totalCapacity,
                                             String preference, Set<Integer> taken) {
        if (totalCapacity <= 0) {
            return new SeatRecommendation(List.of(), 0.0,
                    "No seats configured on this service.");
        }
        String normalized = normalizePreference(preference);
        String pref = normalized == null ? "BALANCED" : normalized;

        Set<Integer> blocked = taken == null ? Set.of() : new HashSet<>(taken);
        List<Integer> candidates = new ArrayList<>();
        for (int seat = 1; seat <= totalCapacity; seat++) {
            if (!blocked.contains(seat)) {
                candidates.add(seat);
            }
        }
        if (candidates.isEmpty()) {
            return new SeatRecommendation(List.of(), 0.0,
                    "No seats available — all remaining seats are already taken.");
        }

        candidates.sort(Comparator.comparingDouble((Integer s) -> -comfort(s, totalCapacity, pref))
                .thenComparing(s -> s));
        int count = Math.max(1, Math.min(seatCount, candidates.size()));
        List<Integer> seats = spreadPick(candidates, count);
        double comfortScore = seats.stream()
                .mapToDouble(s -> comfort(s, totalCapacity, pref))
                .average().orElse(0.0);
        return new SeatRecommendation(sortedSeats(seats), round2(comfortScore),
                reason(pref, count, blocked.size()));
    }

    private double comfort(int seat, int capacity, String pref) {
        int remainder = (seat - 1) % SEATS_PER_ROW;
        boolean window = remainder == 0 || remainder == 3;
        boolean aisle = remainder == 1 || remainder == 2;
        double score = window ? 1.0 : (aisle ? 0.85 : 0.6);
        int rows = Math.max(1, (capacity + SEATS_PER_ROW - 1) / SEATS_PER_ROW);
        double rowPosition = ((seat - 1) / SEATS_PER_ROW) / (double) rows;
        switch (pref) {
            case "WINDOW" -> {
                if (window) {
                    score += 0.35;
                }
            }
            case "AISLE" -> {
                if (aisle) {
                    score += 0.30;
                }
            }
            case "FRONT" -> score += 0.25 * (1.0 - rowPosition);
            case "BACK" -> score += 0.25 * rowPosition;
            case "MIDDLE" -> score += 0.25 * (1.0 - Math.abs(2.0 * rowPosition - 1.0));
            default -> {
                // preference-neutral: position bonus only
            }
        }
        return score;
    }

    private List<Integer> spreadPick(List<Integer> ranked, int count) {
        List<Integer> chosen = new ArrayList<>();
        List<Integer> crowded = new ArrayList<>();
        for (Integer seat : ranked) {
            if (chosen.size() == count) {
                break;
            }
            if (adjacentToChosen(seat, chosen)) {
                crowded.add(seat);
            } else {
                chosen.add(seat);
            }
        }
        for (Integer seat : crowded) {
            if (chosen.size() == count) {
                break;
            }
            chosen.add(seat);
        }
        for (Integer seat : ranked) {
            if (chosen.size() == count) {
                break;
            }
            if (!chosen.contains(seat)) {
                chosen.add(seat);
            }
        }
        return chosen;
    }

    private boolean adjacentToChosen(Integer seat, List<Integer> chosen) {
        for (Integer other : chosen) {
            if (sameRow(seat, other) && Math.abs(seat - other) == 1) {
                return true;
            }
        }
        return false;
    }

    private boolean sameRow(int a, int b) {
        return (a - 1) / SEATS_PER_ROW == (b - 1) / SEATS_PER_ROW;
    }

    private String normalizePreference(String preference) {
        if (preference == null || preference.isBlank()) {
            return null;
        }
        String upper = preference.trim().toUpperCase();
        for (String candidate : PREFERENCES) {
            if (candidate.equals(upper)) {
                return candidate;
            }
        }
        return null;
    }

    private List<Integer> sortedSeats(List<Integer> seats) {
        List<Integer> copy = new ArrayList<>(seats);
        Collections.sort(copy);
        return copy;
    }

    private String reason(String pref, int count, int takenCount) {
        String label = "BALANCED".equals(pref) ? "preference-neutral" : pref.toLowerCase();
        String note = "Selected " + count + " seat(s) optimised for " + label
                + " comfort with window/aisle access and front-row preference.";
        if (takenCount > 0) {
            note += " Already-taken seats were avoided.";
        }
        return note;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}