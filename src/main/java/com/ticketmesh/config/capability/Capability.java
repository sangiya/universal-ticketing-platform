package com.ticketmesh.config.capability;

/**
 * One cell of the explicit capability matrix: a feature that a domain can
 * support and that the platform advertises/validates per vertical.
 *
 * @param name        stable capability key, e.g. seating.inventory
 * @param domain      owning domain key, e.g. TRAIN
 * @param description what the capability enables
 * @param category    capability family, e.g. INVENTORY / TIMING / COMMERCE
 */
public record Capability(
        String name,
        String domain,
        String description,
        String category) {
}