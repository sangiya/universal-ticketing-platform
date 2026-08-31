package com.ticketmesh.scheduler;

import com.ticketmesh.model.Booking;
import com.ticketmesh.model.OrderAuditLog;
import com.ticketmesh.model.Payment;
import com.ticketmesh.model.ProductOrder;
import com.ticketmesh.repository.BookingRepository;
import com.ticketmesh.repository.OrderAuditLogRepository;
import com.ticketmesh.repository.PaymentRepository;
import com.ticketmesh.repository.ProductOrderRepository;
import com.ticketmesh.repository.ProviderProductRepository;
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
    private final ProductOrderRepository productOrderRepository;
    private final ProviderProductRepository productRepository;
    private final OrderAuditLogRepository auditRepository;
    private final long pendingTimeoutMs;
    private final long holdTimeoutMs;

    public ReservationReleaseScheduler(PaymentRepository paymentRepository,
                                       BookingRepository bookingRepository,
                                       BookingService bookingService,
                                       ProductOrderRepository productOrderRepository,
                                       ProviderProductRepository productRepository,
                                       OrderAuditLogRepository auditRepository,
                                       @Value("${app.reservation.pending-timeout-ms:900000}") long pendingTimeoutMs,
                                       @Value("${app.reservation.hold-timeout-ms:1800000}") long holdTimeoutMs) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.bookingService = bookingService;
        this.productOrderRepository = productOrderRepository;
        this.productRepository = productRepository;
        this.auditRepository = auditRepository;
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

        int orderHoldExpired = 0;
        for (ProductOrder order : productOrderRepository
                .findByStatusAndHoldExpiresAtBefore(ProductOrder.Status.PENDING, Instant.now())) {
            order.setStatus(ProductOrder.Status.EXPIRED);
            productOrderRepository.save(order);
            // restore inventory
            try {
                var product = order.getProduct();
                if (product != null) {
                    product.setAvailableQuantity(product.getAvailableQuantity() + order.getQuantity());
                    productRepository.save(product);
                }
            } catch (Exception e) {
                log.warn("Failed to restore inventory for expired order {}", order.getOrderRef(), e);
            }
            auditRepository.save(new OrderAuditLog(order, "PENDING", "EXPIRED", "Hold expired — inventory released", "SYSTEM"));
            orderHoldExpired++;
        }

        if (pendingExpired > 0 || holdExpired > 0 || orderHoldExpired > 0) {
            log.info("Released expired reservations: {} pending payments, {} held bookings, {} marketplace holds",
                    pendingExpired, holdExpired, orderHoldExpired);
        }
    }
}
