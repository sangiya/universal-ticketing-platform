package com.ticketmesh.service;

import com.ticketmesh.dto.BookingResponse;
import com.ticketmesh.dto.CreateBookingRequest;
import com.ticketmesh.exception.ConflictException;
import com.ticketmesh.model.Booking;
import com.ticketmesh.model.TrainRoute;
import com.ticketmesh.model.TrainSchedule;
import com.ticketmesh.model.User;
import com.ticketmesh.repository.BookingRepository;
import com.ticketmesh.repository.TrainScheduleRepository;
import com.ticketmesh.repository.UserRepository;
import com.ticketmesh.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingServiceTest {

    private BookingRepository bookingRepository;
    private TrainScheduleRepository scheduleRepository;
    private UserRepository userRepository;
    private PaymentService paymentService;
    private BookingService bookingService;

    private TrainSchedule schedule;
    private User user;

    @BeforeEach
    void setUp() {
        bookingRepository = mock(BookingRepository.class);
        scheduleRepository = mock(TrainScheduleRepository.class);
        userRepository = mock(UserRepository.class);
        paymentService = mock(PaymentService.class);
        CurrentUser currentUser = mock(CurrentUser.class);

        bookingService = new BookingService(bookingRepository, scheduleRepository,
                userRepository, paymentService, currentUser);

        TrainRoute route = new TrainRoute("COL-KAN", "Colombo-Kandy", "Colombo", "Kandy",
                new BigDecimal("1200.00"), 115);
        schedule = new TrainSchedule(route, "EX-1001", LocalDate.now().plusDays(1),
                LocalTime.of(8, 30), LocalTime.of(12, 15), 60, new BigDecimal("1200.00"));
        user = new User("alice", "encoded", "Alice", "alice@example.com", User.Role.CUSTOMER);

        when(currentUser.username()).thenReturn("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
    }

    @Test
    void create_allocatesLowestFreeSeatAndDecrementsAvailability() {
        when(scheduleRepository.findByIdForUpdate(schedule.getId()))
                .thenReturn(Optional.of(schedule));
        when(bookingRepository.findOccupiedSeats(schedule.getId())).thenReturn(List.of(3, 5));

        BookingResponse response = bookingService.create(request());

        assertEquals(Booking.Status.RESERVED.name(), response.status());
        assertEquals(1, response.seatNumber());
        assertEquals(new BigDecimal("1200.00"), response.fare());
        assertEquals(59, schedule.getAvailableSeats());
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void create_throwsWhenFullyBooked() {
        schedule.setAvailableSeats(0);
        when(scheduleRepository.findByIdForUpdate(schedule.getId()))
                .thenReturn(Optional.of(schedule));

        assertThrows(ConflictException.class, () -> bookingService.create(request()));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void create_rejectsTravelDateMismatch() {
        when(scheduleRepository.findByIdForUpdate(schedule.getId()))
                .thenReturn(Optional.of(schedule));
        CreateBookingRequest req = request();
        req.setTravelDate(LocalDate.now().plusDays(5));

        assertThrows(ConflictException.class, () -> bookingService.create(req));
    }

    @Test
    void cancel_paidBookingRefundsAndFreesSeat() {
        schedule.setAvailableSeats(10);
        Booking booking = new Booking("ticketmesh-REF1234", user, schedule, schedule.getServiceDate(),
                "Alice", 7, new BigDecimal("1200.00"), Booking.Status.PAID);

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(scheduleRepository.findByIdForUpdate(schedule.getId()))
                .thenReturn(Optional.of(schedule));

        BookingResponse response = bookingService.cancel(1L);

        assertEquals(Booking.Status.CANCELLED.name(), response.status());
        assertEquals(11, schedule.getAvailableSeats());
        verify(paymentService).refund(booking);
    }

    @Test
    void cancel_reservedBookingDoesNotRefund() {
        schedule.setAvailableSeats(10);
        Booking booking = new Booking("ticketmesh-REF1235", user, schedule, schedule.getServiceDate(),
                "Alice", 8, new BigDecimal("1200.00"), Booking.Status.RESERVED);

        when(bookingRepository.findById(2L)).thenReturn(Optional.of(booking));
        when(scheduleRepository.findByIdForUpdate(schedule.getId()))
                .thenReturn(Optional.of(schedule));

        BookingResponse response = bookingService.cancel(2L);

        assertEquals(Booking.Status.CANCELLED.name(), response.status());
        verify(paymentService, never()).refund(any());
    }

    private CreateBookingRequest request() {
        CreateBookingRequest req = new CreateBookingRequest();
        req.setScheduleId(schedule.getId());
        req.setTravelDate(schedule.getServiceDate());
        req.setPassengerName("Alice");
        return req;
    }
}
