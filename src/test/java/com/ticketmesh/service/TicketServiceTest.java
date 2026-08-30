package com.ticketmesh.service;

import com.ticketmesh.dto.VerificationResponse;
import com.ticketmesh.exception.ConflictException;
import com.ticketmesh.model.Booking;
import com.ticketmesh.model.TrainRoute;
import com.ticketmesh.model.TrainSchedule;
import com.ticketmesh.model.User;
import com.ticketmesh.repository.BookingRepository;
import com.ticketmesh.repository.UserRepository;
import com.ticketmesh.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TicketServiceTest {

    private BookingRepository bookingRepository;
    private TicketService ticketService;

    private Booking paidBooking;

    @BeforeEach
    void setUp() {
        bookingRepository = mock(BookingRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        CurrentUser currentUser = mock(CurrentUser.class);

        ticketService = new TicketService(bookingRepository, userRepository, currentUser,
                "unit-test-qr-secret-key-2026");

        User user = new User("alice", "encoded", "Alice", "alice@example.com", User.Role.CUSTOMER);
        TrainRoute route = new TrainRoute("COL-KAN", "Colombo-Kandy", "Colombo", "Kandy",
                new BigDecimal("1200.00"), 115);
        TrainSchedule schedule = new TrainSchedule(route, "EX-1001", LocalDate.now().plusDays(1),
                LocalTime.of(8, 30), LocalTime.of(12, 15), 60, new BigDecimal("1200.00"));

        paidBooking = new Booking("ticketmesh-ABCD1234", user, schedule, schedule.getServiceDate(),
                "Alice", 7, new BigDecimal("1200.00"), Booking.Status.PAID);

        when(currentUser.username()).thenReturn("alice");
        when(bookingRepository.findById(any())).thenReturn(Optional.of(paidBooking));
    }

    private String generateValidQr() {
        return ticketService.getTicket(paidBooking.getId()).qrData();
    }

    @Test
    void verify_returnsValidForUntamperedPaidTicket() {
        String qrData = generateValidQr();
        when(bookingRepository.findByBookingRef("ticketmesh-ABCD1234"))
                .thenReturn(Optional.of(paidBooking));

        VerificationResponse response = ticketService.verify(qrData);

        assertTrue(response.valid());
        assertEquals("ticketmesh-ABCD1234", response.bookingRef());
        assertEquals("Alice", response.passengerName());
        assertEquals(7, response.seatNumber());
        assertEquals("Ticket valid", response.message());
    }

    @Test
    void verify_rejectsTamperedSeatNumber() {
        String qrData = generateValidQr();
        String[] parts = qrData.split("\\|");
        parts[1] = "99";
        String tampered = String.join("|", parts);
        when(bookingRepository.findByBookingRef("ticketmesh-ABCD1234"))
                .thenReturn(Optional.of(paidBooking));

        assertThrows(ConflictException.class, () -> ticketService.verify(tampered));
    }

    @Test
    void verify_rejectsForgedSignature() {
        when(bookingRepository.findByBookingRef("UNKNOWN"))
                .thenReturn(Optional.empty());

        assertThrows(ConflictException.class,
                () -> ticketService.verify(
                        "UNKNOWN|5|EX-1001|2026-01-01|Colombo-Kandy|deadbeef"));
    }

    @Test
    void verify_rejectsUnpaidBooking() {
        String qrData = generateValidQr();
        paidBooking.setStatus(Booking.Status.RESERVED);
        when(bookingRepository.findByBookingRef("ticketmesh-ABCD1234"))
                .thenReturn(Optional.of(paidBooking));

        VerificationResponse response = ticketService.verify(qrData);

        assertTrue(!response.valid());
        assertEquals("Ticket invalid or not paid", response.message());
    }

    @Test
    void getTicket_rejectsUnpaidBooking() {
        paidBooking.setStatus(Booking.Status.RESERVED);

        assertThrows(ConflictException.class,
                () -> ticketService.getTicket(paidBooking.getId()));
    }
}
