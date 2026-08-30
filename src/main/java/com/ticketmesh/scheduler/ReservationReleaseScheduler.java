package com.ticketmesh.scheduler;

import com.ticketmesh.model.Booking;
import com.ticketmesh.model.Payment;
import com.ticketmesh.repository.BookingRepository;
import com.ticketmesh.repository.PaymentRepository;
import com.ticketmesh.service.BookingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class ReservationReleaseScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReservationReleaseScheduler.class);

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final BookingService bookingService;
    private final long pendingTimeoutMs;
    private final long holdTimeoutMs;

    public ReservationReleaseScheduler(PaymentRepository paymentRepository,
                                       BookingRepository bookingRepository,
                                       BookingService bookingService,
                                       @Value("${app.reservation.pending-timeout-ms:900000}") long pendingTimeoutMs,
                                       @Value("${app.reservation.hold-timeout-ms:1800000}") long holdTimeoutMs) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.bookingService = bookingService;
        this.pendingTimeoutMs = pendingTimeoutMs;
        this.holdTimeoutMs = holdTimeoutMs;
    }

    @Scheduled(fixedDelayString = "${app.reservation.scheduler-interval-ms:60000}")
    @Transactional
    public void releaseExpiredReservations() {
        Instant pendingCutoff = Instant.now().minusMillis(pendingTimeoutMs);
        Instant holdCutoff = Instant.now().minusMillis(holdTimeoutMs);

        int pendingExpired = 0;
        for (Payment payment : paymentRepository
                .findByStatusAndCreatedAtBefore(Payment.Status.PENDING, pendingCutoff)) {
            payment.setStatus(Payment.Status.EXPIRED);
            paymentRepository.save(payment);
            pendingExpired++;

            Booking booking = payment.getBooking();
            if (booking.getStatus() == Booking.Status.RESERVED) {
                bookingService.expireBooking(booking);
            }
        }

        int holdExpired = 0;
        for (Booking booking : bookingRepository
                .findByStatusAndCreatedAtBefore(Booking.Status.RESERVED, holdCutoff)) {
            bookingService.expireBooking(booking);
            holdExpired++;
        }

        if (pendingExpired > 0 || holdExpired > 0) {
            log.info("Released expired reservations: {} pending payments, {} held bookings",
                    pendingExpired, holdExpired);
        }
    }
}
