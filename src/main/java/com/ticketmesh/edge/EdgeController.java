package com.ticketmesh.edge;

import com.ticketmesh.dto.EdgeHealthResponse;
import com.ticketmesh.dto.MetricsSnapshot;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;

/**
 * Gateway-style edge probe. /health reports the edge is up together with the
 * configured rate-limit rules; /metrics streams a real JVM metrics snapshot
 * read from management beans - no extra runtime dependencies.
 */
@RestController
@RequestMapping("/api/edge")
public class EdgeController {

    private final EdgeRateLimiter edgeRateLimiter;

    public EdgeController(EdgeRateLimiter edgeRateLimiter) {
        this.edgeRateLimiter = edgeRateLimiter;
    }

    @GetMapping("/health")
    public ResponseEntity<EdgeHealthResponse> health() {
        return ResponseEntity.ok(new EdgeHealthResponse("UP", edgeRateLimiter.rules()));
    }

    @GetMapping("/metrics")
    public ResponseEntity<MetricsSnapshot> metrics() {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        return ResponseEntity.ok(new MetricsSnapshot(
                runtime.getUptime() / 1000L,
                threads.getThreadCount(),
                heap.getUsed(),
                heap.getMax()));
    }
}