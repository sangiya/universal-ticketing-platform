package com.ticketmesh.integration;

import com.ticketmesh.dto.PaymentRequest;
import com.ticketmesh.dto.PaymentResponse;
import com.ticketmesh.model.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

/**
 * Lenient fallback for methods without a dedicated gateway (BANK, UPI, ...).
 * Always authorizes offline so none-wallet/none-card flows stay usable.
 */
class OfflineGatewayStub implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(OfflineGatewayStub.class);

    @Override
    public String method() {
        return "STUB";
    }

    @Override
    public PaymentResponse authorize(Payment payment, PaymentRequest request) {
        payment.setProviderRef(payment.getMethod() + "-AUTH-" + payment.getPaymentRef());
        log.info("{} gateway authorized payment {} (offline stub)",
                payment.getMethod(), payment.getPaymentRef());
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