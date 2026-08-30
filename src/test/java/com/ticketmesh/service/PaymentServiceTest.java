package com.ticketmesh.service;

import com.ticketmesh.dto.PaymentRequest;
import com.ticketmesh.dto.PaymentResponse;
import com.ticketmesh.dto.SettleRequest;
import com.ticketmesh.exception.ConflictException;
import com.ticketmesh.exception.InvalidPaymentException;
import com.ticketmesh.model.Booking;
import com.ticketmesh.model.Payment;
import com.ticketmesh.model.TrainRoute;
import com.ticketmesh.model.TrainSchedule;
import com.ticketmesh.model.User;
import com.ticketmesh.repository.BookingRepository;
import com.ticketmesh.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentServiceTest {

    private PaymentRepository paymentRepository;
    private BookingRepository bookingRepository;
    private PaymentService paymentService;

    private Booking booking;
    private Payment pendingPayment;

    @BeforeEach
    void setUp() {
        paymentRepository = mock(PaymentRepository.class);
        bookingRepository = mock(BookingRepository.class);
        paymentService = new PaymentService(paymentRepository, bookingRepository);

        User user = new User("alice", "encoded", "Alice", "alice@example.com", User.Role.CUSTOMER);
        TrainRoute route = new TrainRoute("COL-KAN", "Colombo-Kandy", "Colombo", "Kandy",
                new BigDecimal("1200.00"), 115);
        TrainSchedule schedule = new TrainSchedule(route, "EX-1001", LocalDate.now().plusDays(1),
                LocalTime.of(8, 30), LocalTime.of(12, 15), 60, new BigDecimal("1200.00"));

        booking = new Booking("ticketmesh-PAY1", user, schedule, schedule.getServiceDate(),
                "Alice", 7, new BigDecimal("1200.00"), Booking.Status.RESERVED);

        pendingPayment = new Payment(booking, new BigDecimal("1200.00"), "LKR", "CARD", "pay-123");
        pendingPayment.setCardLast4("1234");

        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(pendingPayment));
        when(paymentRepository.findByBookingId(booking.getId())).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private PaymentRequest cardRequest() {
        PaymentRequest req = new PaymentRequest();
        req.setAmount(new BigDecimal("1200.00"));
        req.setMethod("CARD");
        req.setCardNumber("4111111111111234");
        req.setCardExpiry("12/28");
        req.setCardCvv("123");
        return req;
    }

    private SettleRequest settle(String outcome) {
        SettleRequest req = new SettleRequest();
        req.setOutcome(outcome);
        return req;
    }

    @Test
    void initiate_createsPendingPayment() {
        PaymentResponse response = paymentService.initiate(booking.getId(), cardRequest());

        assertEquals("PENDING", response.status());
        assertEquals("1234", response.cardLast4());
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void initiate_rejectsAmountMismatch() {
        PaymentRequest req = cardRequest();
        req.setAmount(new BigDecimal("999.00"));

        assertThrows(InvalidPaymentException.class,
                () -> paymentService.initiate(booking.getId(), req));
    }

    @Test
    void initiate_rejectsPaymentForPaidBooking() {
        booking.setStatus(Booking.Status.PAID);
        when(paymentRepository.findByBookingId(booking.getId()))
                .thenReturn(Optional.of(pendingPayment));

        assertThrows(ConflictException.class,
                () -> paymentService.initiate(booking.getId(), cardRequest()));
    }

    @Test
    void settleSuccess_marksPaymentSuccessAndBookingPaid() {
        pendingPayment.setStatus(Payment.Status.PENDING);

        PaymentResponse response = paymentService.settle(1L, settle("SUCCESS"));

        assertEquals("SUCCESS", response.status());
        assertEquals(Booking.Status.PAID, booking.getStatus());
    }

    @Test
    void settleFailure_keepsBookingReserved() {
        pendingPayment.setStatus(Payment.Status.PENDING);

        PaymentResponse response = paymentService.settle(1L, settle("FAILED"));

        assertEquals("FAILED", response.status());
        assertEquals(Booking.Status.RESERVED, booking.getStatus());
    }

    @Test
    void settle_rejectsNonPendingPayment() {
        pendingPayment.setStatus(Payment.Status.SUCCESS);

        assertThrows(ConflictException.class, () -> paymentService.settle(1L, settle("FAILED")));
    }

    @Test
    void refund_marksSuccessfulPaymentRefunded() {
        pendingPayment.setStatus(Payment.Status.SUCCESS);
        when(paymentRepository.findByBookingId(booking.getId()))
                .thenReturn(Optional.of(pendingPayment));

        paymentService.refund(booking);

        assertEquals(Payment.Status.REFUNDED, pendingPayment.getStatus());
        verify(paymentRepository).save(pendingPayment);
    }
}
