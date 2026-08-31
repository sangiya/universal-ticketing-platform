package com.ticketmesh.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;

public record SavedTravelerRequest(
        String fullName,
        String relationship,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate dateOfBirth,
        String gender,
        String nationality,
        String documentType,
        String documentNumber,
        String phone,
        String email,
        Boolean consentGiven
) {}
