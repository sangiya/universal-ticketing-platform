package com.ticketmesh.integration;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves a {@link PaymentGateway} by method name (case-insensitive), falling
 * back to a lenient offline stub for unknown methods such as BANK or UPI.
 */
@Component
public class PaymentGatewayRegistry {

    private final Map<String, PaymentGateway> gateways;
    private final PaymentGateway fallback;

    public PaymentGatewayRegistry(List<PaymentGateway> gateways) {
        Map<String, PaymentGateway> byMethod = new HashMap<>();
        for (PaymentGateway gateway : gateways) {
            if (gateway != null && gateway.method() != null) {
                byMethod.put(gateway.method().toUpperCase(), gateway);
            }
        }
        byMethod.putIfAbsent("CARD", new CardGateway());
        byMethod.putIfAbsent("WALLET", new WalletGateway());
        byMethod.putIfAbsent("PAYPAL", new PayPalGateway());
        this.gateways = Collections.unmodifiableMap(byMethod);
        this.fallback = new OfflineGatewayStub();
    }

    public PaymentGateway forMethod(String method) {
        if (method == null) {
            return fallback;
        }
        return gateways.getOrDefault(method.toUpperCase(), fallback);
    }

    public boolean supports(String method) {
        if (method == null) {
            return false;
        }
        return gateways.containsKey(method.toUpperCase());
    }
}