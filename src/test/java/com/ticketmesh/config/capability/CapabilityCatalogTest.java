package com.ticketmesh.config.capability;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CapabilityCatalogTest {

    private final CapabilityCatalog catalog = new CapabilityCatalog();

    @Test
    void trainDomainExposesCoreCapabilities() {
        List<Capability> capabilities = catalog.capabilitiesFor("TRAIN");
        Set<String> names = capabilities.stream()
                .map(Capability::name)
                .collect(Collectors.toSet());
        assertTrue(names.containsAll(Set.of(
                "seating.inventory", "timed.slots", "qr.ticketing",
                "multi.leg", "dynamic.pricing")));
        assertTrue(catalog.hasCapability("TRAIN", "qr.ticketing"));
        assertTrue(catalog.hasCapability("TRAIN", "referrals"));
    }

    @Test
    void capabilitiesDifferPerDomain() {
        assertTrue(catalog.hasCapability("TRAIN", "multi.leg"));
        assertFalse(catalog.hasCapability("MUSEUM", "multi.leg"));
        assertFalse(catalog.hasCapability("BUS", "dynamic.pricing"));
        assertTrue(catalog.hasCapability("MUSEUM", "timed.slots"));
    }

    @Test
    void lookupIsCaseInsensitive() {
        assertTrue(catalog.hasCapability("train", "QR.TICKETING"));
        assertFalse(catalog.capabilitiesFor("train").isEmpty());
    }

    @Test
    void unknownDomainReturnsEmptyMatrix() {
        assertTrue(catalog.capabilitiesFor("SPACESHIP").isEmpty());
        assertFalse(catalog.hasCapability("SPACESHIP", "promotions"));
        assertFalse(catalog.hasCapability("TRAIN", "no.such.capability"));
    }

    @Test
    void blankDomainReturnsMatrixOverview() {
        assertFalse(catalog.capabilitiesFor(null).isEmpty());
        assertFalse(catalog.capabilitiesFor("").isEmpty());
    }
}