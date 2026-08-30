package com.ticketmesh.dto;

/**
 * Real JVM operational metrics snapshot, read from management beans without
 * any extra runtime dependency. Exposed at the edge probe for health boards.
 */
public record MetricsSnapshot(
        long uptimeSeconds,
        int activeThreads,
        long heapUsedBytes,
        long heapMaxBytes) {
}