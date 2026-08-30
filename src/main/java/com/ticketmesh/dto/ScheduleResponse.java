package com.ticketmesh.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.math.BigDecimal;

public record ScheduleResponse(
        Long id,
        String trainCode,
        String routeCode,
        String trainName,
        String origin,
        String destination,
        LocalDate serviceDate,
        LocalTime departureTime,
        LocalTime arrivalTime,
        int capacity,
        int availableSeats,
        BigDecimal fare) {
}
