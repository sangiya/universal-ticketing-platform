package com.ticketmesh.config.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DomainRegistryTest {

    private final DomainRegistry registry = new DomainRegistry();

    @Test
    void listsAllCoreVerticals() {
        List<DomainDefinition> domains = registry.list();
        Set<String> keys = domains.stream()
                .map(DomainDefinition::key)
                .collect(Collectors.toSet());
        assertTrue(keys.containsAll(Set.of(
                "TRAIN", "BUS", "AIR", "EVENT", "CINEMA", "MUSEUM")));
    }

    @Test
    void looksUpDomainByKey() {
        var train = registry.byKey("TRAIN");
        assertTrue(train.isPresent());
        assertEquals("LKR", train.get().defaultCurrency());
        assertTrue(train.get().supportsTimedSlots());
        assertTrue(train.get().supportsInventory());
    }

    @Test
    void lookupIsCaseInsensitive() {
        assertTrue(registry.byKey("train").isPresent());
        assertEquals("MUSEUM", registry.byKey("museum").get().key());
        assertFalse(registry.byKey("museum").get().supportsTimedSlots());
    }

    @Test
    void unknownKeyReturnsEmpty() {
        assertTrue(registry.byKey("SPACESHIP").isEmpty());
        assertTrue(registry.byKey(null).isEmpty());
    }
}