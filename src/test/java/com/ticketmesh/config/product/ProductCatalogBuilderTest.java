package com.ticketmesh.config.product;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductCatalogBuilderTest {

    private final ProductCatalogBuilder builder = new ProductCatalogBuilder();

    @Test
    void resolvesSeatEventTemplate() {
        Optional<ProductTemplate> template = builder.templateFor("SEAT_EVENT");
        assertTrue(template.isPresent());
        assertEquals("Seated Event Ticket", template.get().label());
        assertTrue(template.get().fields().contains("seatNumber"));
        assertTrue(template.get().fields().contains("dateTime"));
    }

    @Test
    void resolvesEveryRegisteredKind() {
        for (String kind : List.of("TRAIN", "BUS", "AIR", "CINEMA",
                "CLASS", "GENERAL", "MUSEUM", "SEAT_EVENT")) {
            assertTrue(builder.templateFor(kind).isPresent(), "missing template for " + kind);
        }
    }

    @Test
    void lookupIsCaseInsensitive() {
        assertTrue(builder.templateFor("train").isPresent());
        assertEquals("Train Journey Ticket", builder.templateFor("TRAIN").get().label());
    }

    @Test
    void unknownKindReturnsEmpty() {
        assertTrue(builder.templateFor("SPACESHIP").isEmpty());
        assertTrue(builder.templateFor(null).isEmpty());
    }

    @Test
    void allTemplatesExposeFields() {
        List<ProductTemplate> templates = builder.allTemplates();
        assertTrue(templates.size() >= 6);
        assertTrue(templates.stream().allMatch(t -> t.kind() != null && !t.fields().isEmpty()));
    }
}