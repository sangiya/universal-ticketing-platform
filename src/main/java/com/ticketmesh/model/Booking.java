package com.ticketmesh.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 40)
    @Column(nullable = false, unique = true, length = 40)
    private String bookingRef;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_id", nullable = false)
    private TrainSchedule schedule;

    @NotNull
    @Column(nullable = false)
    private LocalDate travelDate;

    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String passengerName;

    @Positive
    @Column(nullable = false)
    private int seatNumber;

    @NotNull
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal fare;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column
    private Instant cancelledAt;

    @OneToOne(mappedBy = "booking")
    private Payment payment;

    public Booking() {
    }

    public Booking(String bookingRef, User user, TrainSchedule schedule, LocalDate travelDate,
                   String passengerName, int seatNumber, BigDecimal fare, Status status) {
        this.bookingRef = bookingRef;
        this.user = user;
        this.schedule = schedule;
        this.travelDate = travelDate;
        this.passengerName = passengerName;
        this.seatNumber = seatNumber;
        this.fare = fare;
        this.status = status;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getBookingRef() {
        return bookingRef;
    }

    public User getUser() {
        return user;
    }

    public TrainSchedule getSchedule() {
        return schedule;
    }

    public LocalDate getTravelDate() {
        return travelDate;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public int getSeatNumber() {
        return seatNumber;
    }

    public BigDecimal getFare() {
        return fare;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public Payment getPayment() {
        return payment;
    }

    public enum Status {
        RESERVED,
        PAID,
        CANCELLED,
        EXPIRED
    }
}
