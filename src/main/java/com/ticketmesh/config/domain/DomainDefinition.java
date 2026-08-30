package com.ticketmesh.config.domain;

/**
 * A supported vertical of the universal ticketing platform. Verticals are
 * configuration, not code: a new domain can be switched on by adding a row to
 * the registry without changing any flow.
 *
 * @param key                stable domain key, e.g. TRAIN / BUS / AIR / EVENT
 * @param displayName        human-readable vertical name
 * @param supportsInventory  whether the domain tracks sellable stock
 * @param supportsTimedSlots whether the domain sells timed entry / departure slots
 * @param defaultCurrency    ISO 4217 fallback currency for the vertical
 */
public record DomainDefinition(
        String key,
        String displayName,
        boolean supportsInventory,
        boolean supportsTimedSlots,
        String defaultCurrency) {
}