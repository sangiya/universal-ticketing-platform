package com.ticketmesh.dto;

import java.time.Instant;
import java.time.LocalDate;

public record VerificationResponse(
        boolean valid,
        String bookingRef,
        String passengerName,
        String trainCode,
        String origin,
        String destination,
        LocalDate travelDate,
        int seatNumber,
        String status,
        String message) {
}
