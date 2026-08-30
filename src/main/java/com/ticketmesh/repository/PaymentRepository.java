package com.ticketmesh.repository;

import com.ticketmesh.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPaymentRef(String paymentRef);

    Optional<Payment> findByBookingId(Long bookingId);

    List<Payment> findByStatusAndCreatedAtBefore(Payment.Status status, Instant before);
}
