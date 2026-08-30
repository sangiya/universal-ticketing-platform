package com.ticketmesh.repository;

import com.ticketmesh.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Booking> findByBookingRef(String bookingRef);

    boolean existsByScheduleIdAndSeatNumber(Long scheduleId, int seatNumber);

    @Query("select b.seatNumber from Booking b where b.schedule.id = :scheduleId " +
           "and b.status in (com.ticketmesh.model.Booking.Status.RESERVED, com.ticketmesh.model.Booking.Status.PAID)")
    List<Integer> findOccupiedSeats(@Param("scheduleId") Long scheduleId);

    List<Booking> findByStatusAndCreatedAtBefore(Booking.Status status, Instant before);
}
