package com.ticketmesh.service;

import com.ticketmesh.dto.PaymentRequest;
import com.ticketmesh.dto.PaymentResponse;
import com.ticketmesh.dto.PaymentStatusResponse;
import com.ticketmesh.dto.SettleRequest;
import com.ticketmesh.exception.ConflictException;
import com.ticketmesh.exception.InvalidPaymentException;
import com.ticketmesh.exception.NotFoundException;
import com.ticketmesh.integration.PaymentGateway;
import com.ticketmesh.integration.PaymentGatewayRegistry;
import com.ticketmesh.model.Booking;
import com.ticketmesh.model.Payment;
import com.ticketmesh.repository.BookingRepository;
import com.ticketmesh.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;

@Service
public class PaymentService {

    private static final String CURRENCY = "LKR";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final PaymentGatewayRegistry gatewayRegistry;

    @Autowired
    public PaymentService(PaymentRepository paymentRepository,
                          BookingRepository bookingRepository,
                          PaymentGatewayRegistry gatewayRegistry) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.gatewayRegistry = gatewayRegistry;
    }

    public PaymentService(PaymentRepository paymentRepository,
                          BookingRepository bookingRepository) {
        this(paymentRepository, bookingRepository,
                new PaymentGatewayRegistry(List.of()));
    }

    @Transactional
    public PaymentResponse initiate(Long bookingId, PaymentRequest request) {
        Booking booking = loadBooking(bookingId);

        if (booking.getStatus() == Booking.Status.PAID) {
            throw new ConflictException("Booking already paid: " + booking.getBookingRef());
        }
        if (booking.getStatus() == Booking.Status.CANCELLED
                || booking.getStatus() == Booking.Status.EXPIRED) {
            throw new InvalidPaymentException(
                    "Cannot pay for a " + booking.getStatus().name().toLowerCase()
                            + " booking: " + booking.getBookingRef());
        }
        if (request.getAmount().compareTo(booking.getFare()) != 0) {
            throw new InvalidPaymentException(
                    "Payment amount " + request.getAmount()
                            + " does not match fare " + booking.getFare());
        }

        return paymentRepository.findByBookingId(bookingId)
                .map(p -> resumeOrReturn(booking, p))
                .orElseGet(() -> createPendingPayment(booking, request));
    }

    private PaymentResponse resumeOrReturn(Booking booking, Payment existing) {
        if (existing.getStatus() == Payment.Status.SUCCESS) {
            return toResponse(existing);
        }
        if (existing.getStatus() == Payment.Status.PENDING) {
            return toResponse(existing);
        }
        existing.setStatus(Payment.Status.PENDING);
        existing.setProviderRef("PROV-" + System.nanoTime());
        paymentRepository.save(existing);
        return toResponse(existing);
    }

    private PaymentResponse createPendingPayment(Booking booking, PaymentRequest request) {
        String method = request.getMethod().trim().toUpperCase();
        String cardLast4 = validateCard(method, request);

        Payment payment = new Payment(
                booking,
                request.getAmount(),
                CURRENCY,
                method,
                generateRef("pay"));
        payment.setCardLast4(cardLast4);
        PaymentGateway gateway = gatewayRegistry.forMethod(method);
        gateway.authorize(payment, request);
        paymentRepository.save(payment);
        return toResponse(payment);
    }

    @Transactional
    public PaymentResponse settle(Long paymentId, SettleRequest request) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Payment not found: " + paymentId));
        Booking booking = payment.getBooking();

        if (payment.getStatus() != Payment.Status.PENDING) {
            throw new ConflictException("Payment is not pending (current: "
                    + payment.getStatus().name() + ")");
        }

        if ("SUCCESS".equals(request.getOutcome())) {
            if (booking.getStatus() != Booking.Status.RESERVED) {
                throw new InvalidPaymentException(
                        "Booking is no longer reservable: " + booking.getBookingRef());
            }
            payment.setStatus(Payment.Status.SUCCESS);
            payment.setPaidAt(Instant.now());
            payment.setProviderRef(request.getReference() != null
                    ? request.getReference() : payment.getProviderRef());
            booking.setStatus(Booking.Status.PAID);
        } else {
            payment.setStatus(Payment.Status.FAILED);
            payment.setProviderRef(request.getReference() != null
                    ? request.getReference() : payment.getProviderRef());
        }
        paymentRepository.save(payment);
        bookingRepository.save(booking);
        return toResponse(payment);
    }

    @Transactional
    public void refund(Booking booking) {
        paymentRepository.findByBookingId(booking.getId())
                .filter(p -> p.getStatus() == Payment.Status.SUCCESS)
                .ifPresent(p -> {
                    p.setStatus(Payment.Status.REFUNDED);
                    paymentRepository.save(p);
                });
    }

    public PaymentStatusResponse status(Long bookingId) {
        Booking booking = loadBooking(bookingId);
        Payment payment = paymentRepository.findByBookingId(bookingId).orElse(null);
        if (payment == null) {
            return new PaymentStatusResponse(
                    booking.getId(), booking.getBookingRef(),
                    booking.getStatus().name(), null, null,
                    booking.getFare(), CURRENCY, null, null,
                    null, null);
        }
        return new PaymentStatusResponse(
                booking.getId(), booking.getBookingRef(), booking.getStatus().name(),
                payment.getId(), payment.getStatus().name(),
                payment.getAmount(), payment.getCurrency(), payment.getMethod(),
                payment.getCardLast4(), payment.getCreatedAt(), payment.getPaidAt());
    }

    private Booking loadBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found: " + bookingId));
        return booking;
    }

    private String validateCard(String method, PaymentRequest request) {
        if (!"CARD".equals(method)) {
            return null;
        }
        if (request.getCardNumber() == null || request.getCardNumber().isBlank()
                || request.getCardExpiry() == null || request.getCardExpiry().isBlank()
                || request.getCardCvv() == null || request.getCardCvv().isBlank()) {
            throw new InvalidPaymentException(
                    "CARD payment requires cardNumber, cardExpiry and cardCvv");
        }
        if (request.getCardNumber().length() < 13 || request.getCardNumber().length() > 16) {
            throw new InvalidPaymentException("Invalid card number");
        }
        return request.getCardNumber()
                .substring(request.getCardNumber().length() - 4);
    }

    private String generateRef(String prefix) {
        return prefix + "-" + System.currentTimeMillis()
                + "-" + String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    private PaymentResponse toResponse(Payment p) {
        return new PaymentResponse(
                p.getId(),
                p.getPaymentRef(),
                p.getBooking().getBookingRef(),
                p.getAmount(),
                p.getCurrency(),
                p.getMethod(),
                p.getStatus().name(),
                p.getCardLast4(),
                p.getPaidAt());
    }
}
