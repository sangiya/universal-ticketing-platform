package com.ticketmesh.config.capability;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Explicit per-domain capability matrix. Capabilities are declared here so the
 * checkout, refund, channel and social flows can ask "does this domain support
 * X?" at runtime instead of hard-coding per-vertical branches.
 */
@Component
public class CapabilityCatalog {

    private static final Map<String, List<Capability>> MATRIX = Map.of(
            "TRAIN", List.of(
                    new Capability("seating.inventory", "TRAIN", "Seat-level inventory and tracking", "INVENTORY"),
                    new Capability("timed.slots", "TRAIN", "Fixed departure/slot times", "TIMING"),
                    new Capability("qr.ticketing", "TRAIN", "QR-gated travel entry", "DELIVERY"),
                    new Capability("refund.policy", "TRAIN", "Configurable refund windows", "POLICY"),
                    new Capability("multi.leg", "TRAIN", "Multi-connection journey planning", "ROUTING"),
                    new Capability("promotions", "TRAIN", "Coupon and offer engine", "COMMERCE"),
                    new Capability("family.groups", "TRAIN", "Shared family benefits", "SOCIAL"),
                    new Capability("referrals", "TRAIN", "Invite-friends rewards", "SOCIAL"),
                    new Capability("omnichannel.whatsapp", "TRAIN", "WhatsApp self-service", "CHANNEL"),
                    new Capability("dynamic.pricing", "TRAIN", "Demand-driven fare modulation", "PRICING")),
            "BUS", List.of(
                    new Capability("seating.inventory", "BUS", "Seat-level inventory and tracking", "INVENTORY"),
                    new Capability("timed.slots", "BUS", "Fixed departure/slot times", "TIMING"),
                    new Capability("qr.ticketing", "BUS", "QR-gated boarding", "DELIVERY"),
                    new Capability("promotions", "BUS", "Coupon and offer engine", "COMMERCE"),
                    new Capability("family.groups", "BUS", "Shared family benefits", "SOCIAL"),
                    new Capability("omnichannel.whatsapp", "BUS", "WhatsApp self-service", "CHANNEL")),
            "AIR", List.of(
                    new Capability("seating.inventory", "AIR", "Flight seat allocation", "INVENTORY"),
                    new Capability("timed.slots", "AIR", "Scheduled departures", "TIMING"),
                    new Capability("multi.leg", "AIR", "Connecting flight planning", "ROUTING"),
                    new Capability("refund.policy", "AIR", "Fare-class refund rules", "POLICY"),
                    new Capability("dynamic.pricing", "AIR", "Revenue-managed fare classes", "PRICING"),
                    new Capability("promotions", "AIR", "Coupon and offer engine", "COMMERCE")),
            "EVENT", List.of(
                    new Capability("seating.inventory", "EVENT", "Section/seat inventory per show", "INVENTORY"),
                    new Capability("timed.slots", "EVENT", "Event date/time slots", "TIMING"),
                    new Capability("qr.ticketing", "EVENT", "QR-gated venue entry", "DELIVERY"),
                    new Capability("refund.policy", "EVENT", "Event refund and resale rules", "POLICY"),
                    new Capability("dynamic.pricing", "EVENT", "Demand-driven tiered pricing", "PRICING"),
                    new Capability("referrals", "EVENT", "Invite-friends rewards", "SOCIAL"),
                    new Capability("omnichannel.whatsapp", "EVENT", "WhatsApp self-service", "CHANNEL")),
            "CINEMA", List.of(
                    new Capability("seating.inventory", "CINEMA", "Screen seat allocation", "INVENTORY"),
                    new Capability("timed.slots", "CINEMA", "Showtime slots", "TIMING"),
                    new Capability("qr.ticketing", "CINEMA", "QR-gated theatre entry", "DELIVERY"),
                    new Capability("promotions", "CINEMA", "Coupon and offer engine", "COMMERCE"),
                    new Capability("family.groups", "CINEMA", "Shared family benefits", "SOCIAL")),
            "MUSEUM", List.of(
                    new Capability("timed.slots", "MUSEUM", "Timed entry admission", "TIMING"),
                    new Capability("qr.ticketing", "MUSEUM", "QR-gated admission", "DELIVERY"),
                    new Capability("refund.policy", "MUSEUM", "Configurable refund windows", "POLICY"),
                    new Capability("promotions", "MUSEUM", "Coupon and offer engine", "COMMERCE")));

    private static final List<Capability> ALL = MATRIX.values().stream()
            .flatMap(List::stream)
            .collect(Collectors.collectingAndThen(
                    Collectors.toMap(Capability::name, Function.identity(),
                            (first, duplicate) -> first, LinkedHashMap::new),
                    map -> List.copyOf(map.values())));

    /**
     * Resolve capabilities for a domain. Passing null/blank returns the
     * deduplicated capability surface across all domains ("matrix overview").
     */
    public List<Capability> capabilitiesFor(String domainKey) {
        if (domainKey == null || domainKey.isBlank()) {
            return ALL;
        }
        return MATRIX.getOrDefault(domainKey.trim().toUpperCase(), List.of());
    }

    public boolean hasCapability(String domainKey, String capabilityName) {
        if (domainKey == null || domainKey.isBlank()
                || capabilityName == null || capabilityName.isBlank()) {
            return false;
        }
        return capabilitiesFor(domainKey).stream()
                .anyMatch(c -> c.name().equalsIgnoreCase(capabilityName));
    }
}