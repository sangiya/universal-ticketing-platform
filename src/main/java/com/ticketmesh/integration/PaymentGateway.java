package com.ticketmesh.integration;

import com.ticketmesh.dto.PaymentRequest;
import com.ticketmesh.dto.PaymentResponse;
import com.ticketmesh.model.Payment;

/**
 * A payment method adapter. Implementations are deterministic/offline in this
 * build: {@link #authorize} simulates a successful gateway authorization so the
 * platform runs without real payment credentials.
 */
public interface PaymentGateway {

    String method();

    PaymentResponse authorize(Payment payment, PaymentRequest request);
}