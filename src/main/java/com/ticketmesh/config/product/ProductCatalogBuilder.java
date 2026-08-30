package com.ticketmesh.config.product;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Table-driven product catalog: every sellable product kind maps to a field
 * template. The commerce flows read the template instead of branching per
 * kind, which is what makes adding new product types a no-code operation.
 */
@Component
public class ProductCatalogBuilder {

    private static final Map<String, ProductTemplate> TEMPLATES = Map.of(
            "SEAT_EVENT", new ProductTemplate("SEAT_EVENT", "Seated Event Ticket",
                    List.of("seatNumber", "classType", "dateTime", "venue", "priceBand")),
            "TRAIN", new ProductTemplate("TRAIN", "Train Journey Ticket",
                    List.of("date", "origin", "destination", "classType", "coachType")),
            "BUS", new ProductTemplate("BUS", "Bus Journey Ticket",
                    List.of("date", "origin", "destination", "seatNumber", "serviceType")),
            "AIR", new ProductTemplate("AIR", "Flight Ticket",
                    List.of("date", "flightNumber", "classType", "origin", "destination")),
            "CINEMA", new ProductTemplate("CINEMA", "Cinema Seat",
                    List.of("showDate", "showTime", "screen", "seatNumber")),
            "CLASS", new ProductTemplate("CLASS", "Class / Workshop Seat",
                    List.of("classDate", "sessionTime", "instructor", "location")),
            "GENERAL", new ProductTemplate("GENERAL", "General Admission",
                    List.of("entryDate", "entryTime", "admissionType")),
            "MUSEUM", new ProductTemplate("MUSEUM", "Timed Museum Entry",
                    List.of("entryDate", "entrySlot", "visitorType")));

    public Optional<ProductTemplate> templateFor(String kind) {
        if (kind == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(TEMPLATES.get(kind.trim().toUpperCase()));
    }

    public List<ProductTemplate> allTemplates() {
        return List.copyOf(TEMPLATES.values());
    }
}