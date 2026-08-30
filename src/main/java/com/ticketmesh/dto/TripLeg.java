package com.ticketmesh.dto;

import java.math.BigDecimal;
import java.time.LocalTime;

public record TripLeg(String from, String to, LocalTime departure, LocalTime arrival,
                      BigDecimal fare) {
}