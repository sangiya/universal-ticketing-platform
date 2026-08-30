package com.ticketmesh.service;

import com.ticketmesh.dto.BookingResponse;
import com.ticketmesh.dto.CreateBookingRequest;
import com.ticketmesh.exception.ConflictException;
import com.ticketmesh.exception.NotFoundException;
import com.ticketmesh.model.Booking;
import com.ticketmesh.model.TrainSchedule;
import com.ticketmesh.model.User;
import com.ticketmesh.repository.BookingRepository;
import com.ticketmesh.repository.TrainScheduleRepository;
import com.ticketmesh.repository.UserRepository;
import com.ticketmesh.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final TrainScheduleRepository scheduleRepository;
    private final UserRepository userRepository;
    private final PaymentService paymentService;
    private final CurrentUser currentUser;

    public BookingService(BookingRepository bookingRepository,
                          TrainScheduleRepository scheduleRepository,
                          UserRepository userRepository,
                          PaymentService paymentService,
                          CurrentUser currentUser) {
        this.bookingRepository = bookingRepository;
        this.scheduleRepository = scheduleRepository;
        this.userRepository = userRepository;
        this.paymentService = paymentService;
        this.currentUser = currentUser;
    }

    @Transactional
    public BookingResponse create(CreateBookingRequest request) {
        TrainSchedule schedule = scheduleRepository.findByIdForUpdate(request.getScheduleId())
                .orElseThrow(() -> new NotFoundException(
                        "Schedule not found: " + request.getScheduleId()));

        if (!schedule.getServiceDate().equals(request.getTravelDate())) {
            throw new ConflictException(
                    "Travel date must match schedule service date: " + schedule.getServiceDate());
        }
        if (schedule.getAvailableSeats() <= 0) {
            throw new ConflictException("No seats available on this train");
        }

        int seatNumber = allocateSeat(schedule.getId());

        User user = userRepository.findByUsername(currentUser.username())
                .orElseThrow(() -> new NotFoundException("Authenticated user not found"));

        Booking booking = new Booking(
                generateBookingRef(),
                user,
                schedule,
                schedule.getServiceDate(),
                request.getPassengerName(),
                seatNumber,
                schedule.getFare(),
                Booking.Status.RESERVED);
        bookingRepository.save(booking);

        schedule.setAvailableSeats(schedule.getAvailableSeats() - 1);
        scheduleRepository.save(schedule);

        return toResponse(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> listMine() {
        User user = userRepository.findByUsername(currentUser.username())
                .orElseThrow(() -> new NotFoundException("Authenticated user not found"));
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BookingResponse getMine(Long bookingId) {
        Booking booking = loadOwned(bookingId);
        return toResponse(booking);
    }

    @Transactional
    public BookingResponse cancel(Long bookingId) {
        Booking booking = loadOwned(bookingId);
        if (booking.getStatus() == Booking.Status.CANCELLED) {
            throw new ConflictException("Booking already cancelled: " + booking.getBookingRef());
        }

        if (booking.getStatus() == Booking.Status.PAID) {
            paymentService.refund(booking);
        }

        booking.setStatus(Booking.Status.CANCELLED);
        booking.setCancelledAt(java.time.Instant.now());
        bookingRepository.save(booking);

        scheduleRepository.findByIdForUpdate(booking.getSchedule().getId())
                .ifPresent(s -> {
                    s.setAvailableSeats(Math.min(s.getCapacity(),
                            s.getAvailableSeats() + 1));
                    scheduleRepository.save(s);
                });

        return toResponse(booking);
    }

    @Transactional
    public void expireBooking(Booking booking) {
        if (booking.getStatus() != Booking.Status.RESERVED) {
            return;
        }
        booking.setStatus(Booking.Status.EXPIRED);
        booking.setCancelledAt(java.time.Instant.now());
        bookingRepository.save(booking);

        scheduleRepository.findByIdForUpdate(booking.getSchedule().getId())
                .ifPresent(s -> {
                    s.setAvailableSeats(Math.min(s.getCapacity(),
                            s.getAvailableSeats() + 1));
                    scheduleRepository.save(s);
                });
    }

    private Booking loadOwned(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found: " + bookingId));
        if (!booking.getUser().getUsername().equals(currentUser.username())) {
            throw new NotFoundException("Booking not found: " + bookingId);
        }
        return booking;
    }

    private int allocateSeat(Long scheduleId) {
        Set<Integer> occupied = bookingRepository.findOccupiedSeats(scheduleId)
                .stream().collect(Collectors.toSet());
        for (int seat = 1; ; seat++) {
            if (!occupied.contains(seat)) {
                return seat;
            }
        }
    }

    private String generateBookingRef() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder("ticketmesh-");
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return sb.toString();
    }

    private BookingResponse toResponse(Booking b) {
        return new BookingResponse(
                b.getId(),
                b.getBookingRef(),
                b.getStatus().name(),
                b.getPassengerName(),
                b.getSeatNumber(),
                b.getFare(),
                b.getSchedule().getTrainCode(),
                b.getSchedule().getRoute().getName(),
                b.getSchedule().getRoute().getOrigin(),
                b.getSchedule().getRoute().getDestination(),
                b.getTravelDate(),
                b.getSchedule().getDepartureTime(),
                b.getSchedule().getArrivalTime(),
                b.getCreatedAt(),
                b.getStatus() == Booking.Status.PAID);
    }
}
