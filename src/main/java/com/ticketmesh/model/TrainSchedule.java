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
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "train_schedules")
public class TrainSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "route_id", nullable = false)
    private TrainRoute route;

    @NotBlank
    @Size(max = 20)
    @Column(nullable = false, length = 20)
    private String trainCode;

    @NotNull
    @Column(nullable = false)
    private LocalDate serviceDate;

    @NotNull
    @Column(nullable = false)
    private LocalTime departureTime;

    @NotNull
    @Column(nullable = false)
    private LocalTime arrivalTime;

    @Positive
    @Column(nullable = false)
    private int capacity;

    @Positive
    @Column(nullable = false)
    private int availableSeats;

    @NotNull
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal fare;

    @Version
    @Column(nullable = false)
    private long version;

    public TrainSchedule() {
    }

    public TrainSchedule(TrainRoute route, String trainCode, LocalDate serviceDate,
                         LocalTime departureTime, LocalTime arrivalTime,
                         int capacity, BigDecimal fare) {
        this.route = route;
        this.trainCode = trainCode;
        this.serviceDate = serviceDate;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.capacity = capacity;
        this.availableSeats = capacity;
        this.fare = fare;
    }

    public Long getId() {
        return id;
    }

    public TrainRoute getRoute() {
        return route;
    }

    public String getTrainCode() {
        return trainCode;
    }

    public LocalDate getServiceDate() {
        return serviceDate;
    }

    public LocalTime getDepartureTime() {
        return departureTime;
    }

    public LocalTime getArrivalTime() {
        return arrivalTime;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getAvailableSeats() {
        return availableSeats;
    }

    public void setAvailableSeats(int availableSeats) {
        this.availableSeats = availableSeats;
    }

    public BigDecimal getFare() {
        return fare;
    }

    public long getVersion() {
        return version;
    }
}
