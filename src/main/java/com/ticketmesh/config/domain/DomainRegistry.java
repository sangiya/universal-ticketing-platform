package com.ticketmesh.config.domain;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Config-driven vertical registry. The product is built as a single engine
 * over many sellable verticals; on-boarding a new vertical (e.g. MUSEUM) is a
 * data change here, not a code change. The registry is compiled-in as
 * versioned configuration so all environments agree on the supported surface.
 */
@Component
public class DomainRegistry {

    private static final List<DomainDefinition> DEFINITIONS = List.of(
            new DomainDefinition("TRAIN", "Rail & Train Travel", true, true, "LKR"),
            new DomainDefinition("BUS", "Intercity & Local Bus", true, true, "LKR"),
            new DomainDefinition("AIR", "Flights & Air Travel", true, true, "USD"),
            new DomainDefinition("EVENT", "Live Events & Concerts", true, true, "USD"),
            new DomainDefinition("CINEMA", "Cinema & Theatre", true, true, "USD"),
            new DomainDefinition("MUSEUM", "Museums & Attractions", true, false, "EUR"));

    private static final Map<String, DomainDefinition> BY_KEY = DEFINITIONS.stream()
            .collect(Collectors.toUnmodifiableMap(
                    DomainDefinition::key, Function.identity()));

    public List<DomainDefinition> list() {
        return DEFINITIONS;
    }

    public Optional<DomainDefinition> byKey(String key) {
        if (key == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_KEY.get(key.trim().toUpperCase()));
    }
}