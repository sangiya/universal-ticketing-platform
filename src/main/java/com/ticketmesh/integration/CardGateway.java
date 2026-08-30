package com.ticketmesh.integration;

import com.ticketmesh.dto.PaymentRequest;
import com.ticketmesh.dto.PaymentResponse;
import com.ticketmesh.model.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Offline/deterministic card gateway. Card fields are validated by
 * {@code PaymentService} before this adapter is invoked.
 */
@Component
public class CardGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(CardGateway.class);

    @Override
    public String method() {
        return "CARD";
    }

    @Override
    public PaymentResponse authorize(Payment payment, PaymentRequest request) {
        payment.setProviderRef("CARD-AUTH-" + payment.getPaymentRef());
        log.info("Card gateway authorized payment {} (offline)", payment.getPaymentRef());
        return success(payment);
    }

    private PaymentResponse success(Payment payment) {
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