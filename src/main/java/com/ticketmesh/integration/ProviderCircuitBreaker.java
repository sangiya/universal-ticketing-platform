package com.ticketmesh.integration;

import com.ticketmesh.dto.UniversalOffer;
import com.ticketmesh.dto.UniversalSearchRequest;
import com.ticketmesh.model.Provider;
import com.ticketmesh.repository.ProviderRepository;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-provider circuit breakers for outbound calls to external provider APIs.
 * When a provider is slow or errors out, its circuit opens; subsequent calls
 * return immediately with an empty result rather than blocking the search
 * thread pool. The circuit half-opens after a cool-down, then closes again
 * once calls succeed.
 *
 * <p>Configuration (defaults below; override via {@code application.yml}):</p>
 * <ul>
 *   <li>{@code failureRateThreshold}: 50%</li>
 *   <li>{@code slowCallRateThreshold}: 50%</li>
 *   <li>{@code slowCallDurationThreshold}: 2s</li>
 *   <li>{@code waitDurationInOpenState}: 30s</li>
 *   <li>{@code slidingWindowSize}: 20</li>
 *   <li>{@code minimumNumberOfCalls}: 5</li>
 * </ul>
 *
 * <p>The {@link ProviderOfferClient} uses this wrapper via
 * {@link #searchWithBreaker(Provider, UniversalSearchRequest, java.util.function.Function)}.</p>
 */
@Component
public class ProviderCircuitBreaker {

    private static final Logger log = LoggerFactory.getLogger(ProviderCircuitBreaker.class);

    private final CircuitBreakerRegistry registry;
    private final ProviderRepository providerRepository;
    private final Map<String, CircuitBreaker> perProvider = new ConcurrentHashMap<>();

    public ProviderCircuitBreaker(ProviderRepository providerRepository) {
        this.providerRepository = providerRepository;
        this.registry = CircuitBreakerRegistry.of(
                CircuitBreakerConfig.custom()
                        .failureRateThreshold(50.0f)
                        .slowCallRateThreshold(50.0f)
                        .slowCallDurationThreshold(Duration.ofSeconds(2))
                        .waitDurationInOpenState(Duration.ofSeconds(30))
                        .slidingWindowSize(20)
                        .minimumNumberOfCalls(5)
                        .permittedNumberOfCallsInHalfOpenState(3)
                        .build());
    }

    public CircuitBreaker forProvider(String providerCode) {
        return perProvider.computeIfAbsent(providerCode, code -> {
            CircuitBreaker cb = registry.circuitBreaker("provider-" + code);
            cb.getEventPublisher()
                    .onStateTransition(e -> log.warn(
                            "Provider {} circuit transition: {} -> {}",
                            code, e.getStateTransition().getFromState(),
                            e.getStateTransition().getToState()))
                    .onError(e -> log.debug(
                            "Provider {} circuit error: {}",
                            code, e.getThrowable().getMessage()));
            return cb;
        });
    }

    /**
     * Run a search call through a provider's circuit breaker. Returns an
     * empty list if the circuit is open or the call fails.
     */
    public List<UniversalOffer> searchWithBreaker(Provider provider,
                                                  UniversalSearchRequest request,
                                                  java.util.function.Function<String, List<UniversalOffer>> searchFn) {
        if (provider == null || provider.getApiEndpoint() == null || provider.getApiEndpoint().isBlank()) {
            return Collections.emptyList();
        }
        if (provider.getStatus() != Provider.Status.ACTIVE) {
            return Collections.emptyList();
        }
        CircuitBreaker cb = forProvider(provider.getCode());
        if (cb.getState() == CircuitBreaker.State.OPEN) {
            log.debug("Provider {} circuit open — skipping search", provider.getCode());
            return Collections.emptyList();
        }
        try {
            return cb.executeSupplier(() -> searchFn.apply(provider.getApiEndpoint()));
        } catch (Exception ex) {
            log.warn("Provider {} call failed: {}", provider.getCode(), ex.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Get health snapshot for all configured provider circuits.
     */
    public Map<String, String> snapshot() {
        Map<String, String> out = new java.util.LinkedHashMap<>();
        for (var entry : perProvider.entrySet()) {
            CircuitBreaker cb = entry.getValue();
            out.put(entry.getKey(), cb.getState().name()
                    + " failures=" + cb.getMetrics().getNumberOfFailedCalls()
                    + "/" + cb.getMetrics().getNumberOfBufferedCalls());
        }
        if (providerRepository != null) {
            for (Provider p : providerRepository.findAll()) {
                if (!out.containsKey(p.getCode())) {
                    out.put(p.getCode(), "CLOSED (idle)");
                }
            }
        }
        return out;
    }
}
