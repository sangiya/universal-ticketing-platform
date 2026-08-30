package com.ticketmesh.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * An ordered leg of a trip referencing a booking (or a reserved leg id).
 */
@Entity
@Table(name = "trip_items")
public class TripItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(name = "booking_id")
    private Long bookingId;

    @NotNull
    @Column(nullable = false)
    private int position;

    @Size(max = 500)
    @Column(length = 500)
    private String note;

    public TripItem() {
    }

    public TripItem(Trip trip, Long bookingId, int position, String note) {
        this.trip = trip;
        this.bookingId = bookingId;
        this.position = position;
        this.note = note;
    }

    public Long getId() {
        return id;
    }

    public Trip getTrip() {
        return trip;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public int getPosition() {
        return position;
    }

    public String getNote() {
        return note;
    }
}
