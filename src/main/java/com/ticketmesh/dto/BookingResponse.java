package com.ticketmesh.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record BookingResponse(
        Long bookingId,
        String bookingRef,
        String status,
        String passengerName,
        int seatNumber,
        BigDecimal fare,
        String trainCode,
        String trainName,
        String origin,
        String destination,
        LocalDate travelDate,
        LocalTime departureTime,
        LocalTime arrivalTime,
        Instant createdAt,
        boolean paid) {
}
