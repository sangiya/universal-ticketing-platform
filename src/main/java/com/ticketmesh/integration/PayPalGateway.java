package com.ticketmesh.integration;

import com.ticketmesh.dto.PaymentRequest;
import com.ticketmesh.dto.PaymentResponse;
import com.ticketmesh.model.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Offline/deterministic PayPal gateway. No card fields are required — the
 * PayPal order id is carried via the caller-provided provider reference.
 */
@Component
public class PayPalGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(PayPalGateway.class);

    @Override
    public String method() {
        return "PAYPAL";
    }

    @Override
    public PaymentResponse authorize(Payment payment, PaymentRequest request) {
        payment.setProviderRef("PAYPAL-AUTH-" + payment.getPaymentRef());
        log.info("PayPal gateway authorized payment {} (offline)", payment.getPaymentRef());
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