package com.ticketmesh.edge;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Deterministic in-memory fixed-window rate limiter for the edge/gateway
 * layer. Each key counts requests inside the current window; the window
 * resets on the boundary, so a burst cannot spill across windows. Thread-safe
 * via per-bucket synchronization over a concurrent map - no locks on the
 * hot path beyond the owning bucket.
 */
@Component
public class EdgeRateLimiter {

    /** Status for a key against a rule, used by callers and the edge probe. */
    public record RateLimitStatus(boolean allowed, int remaining, long resetAtSeconds) {
    }

    public static final RateLimitRule DEFAULT_RULE = new RateLimitRule("client", 60, 60);

    private static final List<RateLimitRule> RULES = List.of(
            new RateLimitRule("api.global", 1000, 60),
            new RateLimitRule("tenant", 300, 60),
            new RateLimitRule("client", 60, 60),
            new RateLimitRule("ip", 20, 60));

    private static final class Bucket {
        private long window;
        private int count;

        private Bucket(long window) {
            this.window = window;
        }
    }

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final Clock clock;

    public EdgeRateLimiter() {
        this(Clock.systemUTC());
    }

    EdgeRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public List<RateLimitRule> rules() {
        return RULES;
    }

    public boolean tryAcquire(String key) {
        return tryAcquire(key, DEFAULT_RULE);
    }

    public boolean tryAcquire(String key, RateLimitRule rule) {
        long epochSecond = clock.instant().getEpochSecond();
        long window = Math.floorDiv(epochSecond, rule.windowSeconds());
        Bucket bucket = buckets.computeIfAbsent(key, ignored -> new Bucket(window));
        synchronized (bucket) {
            if (bucket.window != window) {
                bucket.window = window;
                bucket.count = 0;
            }
            if (bucket.count < rule.limit()) {
                bucket.count++;
                return true;
            }
            return false;
        }
    }

    public RateLimitStatus status(String key) {
        return status(key, DEFAULT_RULE);
    }

    public RateLimitStatus status(String key, RateLimitRule rule) {
        long epochSecond = clock.instant().getEpochSecond();
        long window = Math.floorDiv(epochSecond, rule.windowSeconds());
        Bucket bucket = buckets.get(key);
        int count = bucket != null && bucket.window == window ? bucket.count : 0;
        long resetAtSeconds = (window + 1) * rule.windowSeconds();
        return new RateLimitStatus(count < rule.limit(), rule.limit() - count, resetAtSeconds);
    }
}