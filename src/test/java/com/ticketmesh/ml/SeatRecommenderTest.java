package com.ticketmesh.ml;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeatRecommenderTest {

    private SeatRecommender recommender;

    @BeforeEach
    void setUp() {
        recommender = new SeatRecommender();
    }

    @Test
    void windowPreferencePicksSpacedWindowSeats() {
        SeatRecommendation result = recommender.recommendSeats(2, 24, "WINDOW");

        assertEquals(List.of(1, 4), result.seats());
        assertEquals(2, result.seats().size());
        assertTrue(result.comfortScore() > 0.0);
        assertTrue(result.seats().stream()
                .allMatch(SeatRecommenderTest::isWindowSeat));
        assertTrue(result.reason().contains("window"));
    }

    @Test
    void windowPreferenceIsCaseInsensitiveAndDeterministic() {
        SeatRecommendation lower = recommender.recommendSeats(2, 24, "window");
        SeatRecommendation upper = recommender.recommendSeats(2, 24, "WINDOW");

        assertEquals(upper.seats(), lower.seats());
        assertEquals(upper.comfortScore(), lower.comfortScore(), 1e-9);
    }

    @Test
    void takenSeatsAreAvoided() {
        SeatRecommendation result = recommender.recommendSeats(2, 24, "WINDOW",
                Set.of(1, 4));

        assertEquals(List.of(5, 8), result.seats());
        assertFalse(result.seats().contains(1));
        assertFalse(result.seats().contains(4));
        assertTrue(result.reason().contains("avoided"));
    }

    @Test
    void aislePreferencePicksSpacedAisleSeats() {
        SeatRecommendation result = recommender.recommendSeats(2, 24, "AISLE");

        assertEquals(List.of(2, 6), result.seats());
        assertTrue(result.seats().stream().allMatch(SeatRecommenderTest::isAisleSeat));
        int a = result.seats().get(0);
        int b = result.seats().get(1);
        assertTrue(Math.abs(a - b) > 1, "recommended seats must be spread out");
    }

    @Test
    void frontPreferenceStartsFromTheFirstRows() {
        SeatRecommendation result = recommender.recommendSeats(2, 24, "FRONT");

        assertEquals(List.of(1, 4), result.seats());
        assertTrue(result.reason().contains("front"));
    }

    @Test
    void backPreferenceReachesTheLastRows() {
        SeatRecommendation result = recommender.recommendSeats(2, 24, "back");

        assertEquals(2, result.seats().size());
        assertTrue(result.seats().stream().anyMatch(SeatRecommenderTest::isWindowSeat));
        assertEquals(24, result.seats().get(result.seats().size() - 1));
        assertTrue(result.seats().stream().allMatch(s -> s > 12));
    }

    @Test
    void middlePreferenceStaysInTheCentralRows() {
        SeatRecommendation result = recommender.recommendSeats(2, 24, "MIDDLE");

        assertEquals(2, result.seats().size());
        assertTrue(result.seats().stream().allMatch(s -> s >= 9 && s <= 16));
        assertTrue(result.reason().contains("middle"));
    }

    @Test
    void unknownPreferenceFallsBackToNeutralAndStaysValid() {
        SeatRecommendation result = recommender.recommendSeats(2, 24, "bogus");

        assertEquals(2, result.seats().size());
        assertTrue(result.seats().stream().allMatch(s -> s >= 1 && s <= 24));
        assertTrue(result.reason().contains("preference-neutral"));
    }

    @Test
    void demandLargerThanAvailabilityClampsToAvailableSeats() {
        SeatRecommendation result = recommender.recommendSeats(100, 40, "WINDOW");

        assertEquals(40, result.seats().size());
    }

    @Test
    void noSeatsLeftOrInvalidCapacityReturnsEmptyRecommendation() {
        SeatRecommendation full = recommender.recommendSeats(1, 2, "WINDOW", Set.of(1, 2));
        assertTrue(full.seats().isEmpty());
        assertTrue(full.reason().contains("taken"));

        SeatRecommendation invalid = recommender.recommendSeats(1, 0, "WINDOW");
        assertTrue(invalid.seats().isEmpty());
        assertTrue(invalid.reason().contains("No seats configured"));
    }

    private static boolean isWindowSeat(int seat) {
        int remainder = (seat - 1) % 4;
        return remainder == 0 || remainder == 3;
    }

    private static boolean isAisleSeat(int seat) {
        int remainder = (seat - 1) % 4;
        return remainder == 1 || remainder == 2;
    }
}