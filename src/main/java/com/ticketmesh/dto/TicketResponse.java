package com.ticketmesh.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record TicketResponse(
        String bookingRef,
        String passengerName,
        String trainCode,
        String trainName,
        String origin,
        String destination,
        LocalDate travelDate,
        LocalTime departureTime,
        LocalTime arrivalTime,
        int seatNumber,
        BigDecimal fare,
        String status,
        String qrData) {
}
