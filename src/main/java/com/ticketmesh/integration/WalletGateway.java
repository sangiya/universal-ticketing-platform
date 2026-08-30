package com.ticketmesh.integration;

import com.ticketmesh.dto.PaymentRequest;
import com.ticketmesh.dto.PaymentResponse;
import com.ticketmesh.model.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Offline/deterministic wallet gateway (PayPal wallet / in-app balance / UPI).
 * Requires no card fields; the wallet reference is supplied by the caller.
 */
@Component
public class WalletGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(WalletGateway.class);

    @Override
    public String method() {
        return "WALLET";
    }

    @Override
    public PaymentResponse authorize(Payment payment, PaymentRequest request) {
        payment.setProviderRef("WALLET-AUTH-" + payment.getPaymentRef());
        log.info("Wallet gateway authorized payment {} (offline)", payment.getPaymentRef());
        return new PaymentResponse(
                payment.getId(),
                payment.getPaymentRef(),
                payment.getBooking().getBookingRef(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getMethod(),
                "SUCCESS",
                payment.getCardLast4(),
                Instant.now());
    }
}