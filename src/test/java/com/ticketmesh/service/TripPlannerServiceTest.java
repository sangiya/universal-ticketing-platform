package com.ticketmesh.service;

import com.ticketmesh.dto.TripPlan;
import com.ticketmesh.model.TrainRoute;
import com.ticketmesh.model.TrainSchedule;
import com.ticketmesh.repository.TrainScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TripPlannerServiceTest {

    private TrainScheduleRepository scheduleRepository;
    private TripPlannerService service;

    private final LocalDate date = LocalDate.of(2026, 9, 1);

    @BeforeEach
    void setUp() {
        scheduleRepository = mock(TrainScheduleRepository.class);
        service = new TripPlannerService(scheduleRepository);
    }

    @Test
    void plan_assembleChainedMultiLegItinerary() {
        TrainRoute colomboKandy = route("Colombo", "Kandy");
        TrainRoute kandyElla = route("Kandy", "Ella");
        when(scheduleRepository.findByServiceDate(date)).thenReturn(List.of(
                schedule(colomboKandy, "EX-1001", LocalTime.of(8, 30), LocalTime.of(12, 15),
                        new BigDecimal("1200.00")),
                schedule(kandyElla, "EX-2002", LocalTime.of(13, 0), LocalTime.of(17, 10),
                        new BigDecimal("900.00"))));

        TripPlan plan = service.plan(1L, "Colombo", "Ella", 2, date.toString());

        assertTrue(plan.feasible());
        assertEquals("Colombo", plan.origin());
        assertEquals("Ella", plan.destination());
        assertEquals(2, plan.legCount());
        assertEquals(2, plan.legs().size());
        assertEquals("Colombo", plan.legs().get(0).from());
        assertEquals("Kandy", plan.legs().get(0).to());
        assertEquals(LocalTime.of(8, 30), plan.legs().get(0).departure());
        assertEquals("Kandy", plan.legs().get(1).from());
        assertEquals("Ella", plan.legs().get(1).to());
        assertEquals(LocalTime.of(17, 10), plan.legs().get(1).arrival());
        assertEquals(0, new BigDecimal("2100.00").compareTo(plan.totalFare()));
        assertTrue(plan.feasibilityNote().contains("2-leg"));
    }

    @Test
    void plan_directRouteIsSingleLegAndFeasible() {
        TrainRoute colomboKandy = route("Colombo", "Kandy");
        when(scheduleRepository.findByServiceDate(date)).thenReturn(List.of(
                schedule(colomboKandy, "EX-1001", LocalTime.of(8, 30), LocalTime.of(12, 15),
                        new BigDecimal("1200.00"))));

        TripPlan plan = service.plan(1L, "Colombo", "Kandy", 1, date.toString());

        assertTrue(plan.feasible());
        assertEquals(1, plan.legCount());
        assertEquals(1, plan.legs().size());
        assertEquals(0, new BigDecimal("1200.00").compareTo(plan.totalFare()));
    }

    @Test
    void plan_infeasibleWhenNoChainingPathExistsBetweenServedStations() {
        TrainRoute colomboKandy = route("Colombo", "Kandy");
        TrainRoute gampahaElla = route("Gampaha", "Ella");
        when(scheduleRepository.findByServiceDate(date)).thenReturn(List.of(
                schedule(colomboKandy, "EX-1001", LocalTime.of(8, 30), LocalTime.of(12, 15),
                        new BigDecimal("1200.00")),
                schedule(gampahaElla, "EX-3003", LocalTime.of(9, 0), LocalTime.of(13, 0),
                        new BigDecimal("800.00"))));

        TripPlan plan = service.plan(1L, "Colombo", "Ella", 2, date.toString());

        assertFalse(plan.feasible());
        assertEquals(0, plan.legCount());
        assertTrue(plan.legs().isEmpty());
        assertEquals(0, new BigDecimal("0.00").compareTo(plan.totalFare()));
        assertTrue(plan.feasibilityNote().contains("No combination"));
    }

    @Test
    void plan_unknownDestinationReportsNoOperatingServices() {
        TrainRoute colomboKandy = route("Colombo", "Kandy");
        when(scheduleRepository.findByServiceDate(date)).thenReturn(List.of(
                schedule(colomboKandy, "EX-1001", LocalTime.of(8, 30), LocalTime.of(12, 15),
                        new BigDecimal("1200.00"))));

        TripPlan plan = service.plan(1L, "Colombo", "Jaffna", 2, date.toString());

        assertFalse(plan.feasible());
        assertTrue(plan.feasibilityNote().contains("No services operate"));
        assertTrue(plan.feasibilityNote().contains("Jaffna"));
    }

    @Test
    void plan_connectionMustDepartAfterPreviousArrival() {
        TrainRoute colomboKandy = route("Colombo", "Kandy");
        TrainRoute kandyElla = route("Kandy", "Ella");
        when(scheduleRepository.findByServiceDate(date)).thenReturn(List.of(
                schedule(colomboKandy, "EX-1001", LocalTime.of(8, 30), LocalTime.of(12, 15),
                        new BigDecimal("1200.00")),
                schedule(kandyElla, "EX-2002", LocalTime.of(11, 0), LocalTime.of(15, 0),
                        new BigDecimal("900.00"))));

        TripPlan plan = service.plan(1L, "Colombo", "Ella", 2, date.toString());

        assertFalse(plan.feasible());
        assertTrue(plan.feasibilityNote().contains("No combination"));
    }

    @Test
    void plan_blankOrSameOriginDestinationIsInfeasible() {
        TripPlan blank = service.plan(1L, " ", "Ella", 2, date.toString());
        assertFalse(blank.feasible());
        assertTrue(blank.feasibilityNote().contains("required"));

        TripPlan same = service.plan(1L, "Colombo", "Colombo", 2, date.toString());
        assertFalse(same.feasible());
        assertTrue(same.feasibilityNote().contains("differ"));
    }

    @Test
    void plan_unparseableStartDateIsInfeasibleWithClearNote() {
        TripPlan plan = service.plan(1L, "Colombo", "Kandy", 1, "not-a-date");
        assertFalse(plan.feasible());
        assertTrue(plan.feasibilityNote().contains("Invalid startDate"));
    }

    @Test
    void plan_emptyScheduleReturnsInfeasible() {
        when(scheduleRepository.findByServiceDate(date)).thenReturn(List.of());

        TripPlan plan = service.plan(1L, "Colombo", "Ella", 2, date.toString());

        assertFalse(plan.feasible());
        assertTrue(plan.feasibilityNote().contains("No services operate"));
    }

    private TrainRoute route(String origin, String destination) {
        return new TrainRoute(origin + "-" + destination, origin + " to " + destination,
                origin, destination, new BigDecimal("1000.00"), 100);
    }

    private TrainSchedule schedule(TrainRoute route, String trainCode, LocalTime departure,
                                   LocalTime arrival, BigDecimal fare) {
        return new TrainSchedule(route, trainCode, date, departure, arrival, 60, fare);
    }
}