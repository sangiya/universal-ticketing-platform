package com.ticketmesh.config.product;

import java.util.List;

/**
 * A no-code product template: the fields an admin-defined product of a given
 * kind must collect. Templates are compiled configuration so product
 * creation needs zero code changes.
 *
 * @param kind   stable product kind, e.g. SEAT_EVENT / TRAIN / CLASS / GENERAL
 * @param label  human-readable template name
 * @param fields field names the template collects (e.g. seatNumber, dateTime)
 */
public record ProductTemplate(
        String kind,
        String label,
        List<String> fields) {
}