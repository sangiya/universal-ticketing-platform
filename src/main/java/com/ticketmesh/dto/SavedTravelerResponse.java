package com.ticketmesh.dto;

import java.time.Instant;
import java.time.LocalDate;

public record SavedTravelerResponse(
        Long id,
        String fullName,
        String relationship,
        LocalDate dateOfBirth,
        String gender,
        String nationality,
        String documentType,
        String documentNumberMasked,
        String phoneMasked,
        String emailMasked,
        boolean consentGiven,
        Instant consentAt,
        Instant createdAt
) {}
