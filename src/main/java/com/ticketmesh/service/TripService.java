package com.ticketmesh.service;

import com.ticketmesh.exception.ConflictException;
import com.ticketmesh.exception.NotFoundException;
import com.ticketmesh.model.Booking;
import com.ticketmesh.model.Trip;
import com.ticketmesh.model.TripItem;
import com.ticketmesh.repository.BookingRepository;
import com.ticketmesh.repository.TripItemRepository;
import com.ticketmesh.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Multi-ticket / multi-service trip (smart trip planner). Bundles bookings
 * across domains into a single itinerary so a customer can plan a whole journey.
 */
@Service
public class TripService {

    private final TripRepository tripRepository;
    private final TripItemRepository tripItemRepository;
    private final BookingRepository bookingRepository;

    public TripService(TripRepository tripRepository,
                       TripItemRepository tripItemRepository,
                       BookingRepository bookingRepository) {
        this.tripRepository = tripRepository;
        this.tripItemRepository = tripItemRepository;
        this.bookingRepository = bookingRepository;
    }

    @Transactional
    public Trip create(Long tenantId, Long userId, String title) {
        return tripRepository.save(new Trip(tenantId, userId, title));
    }

    @Transactional
    public Trip addLeg(Long tenantId, Long userId, Long tripId, Long bookingId, String note) {
        Trip trip = requireOwned(tenantId, userId, tripId);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found: " + bookingId));
        if (!booking.getUser().getId().equals(userId)) {
            throw new ConflictException("Booking does not belong to this user");
        }
        int position = tripItemRepository.findByTrip_IdOrderByPosition(trip.getId())
                .size() + 1;
        tripItemRepository.save(new TripItem(trip, booking.getId(), position, note));
        return trip;
    }

    @Transactional(readOnly = true)
    public List<Trip> listMine(Long userId) {
        return tripRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<TripItem> legs(Long tripId) {
        return tripItemRepository.findByTrip_IdOrderByPosition(tripId);
    }

    @Transactional(readOnly = true)
    public int legCount(Long tripId) {
        return tripItemRepository.findByTrip_IdOrderByPosition(tripId).size();
    }

    private Trip requireOwned(Long tenantId, Long userId, Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new NotFoundException("Trip not found: " + tripId));
        if (!trip.getUserId().equals(userId) || !trip.getTenantId().equals(tenantId)) {
            throw new NotFoundException("Trip not found: " + tripId);
        }
        return trip;
    }
}
