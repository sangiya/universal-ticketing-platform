package com.ticketmesh.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Entity
@Table(name = "train_routes")
public class TrainRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 20)
    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String name;

    @NotBlank
    @Size(max = 80)
    @Column(nullable = false, length = 80)
    private String origin;

    @NotBlank
    @Size(max = 80)
    @Column(nullable = false, length = 80)
    private String destination;

    @NotNull
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal baseFare;

    @Column(nullable = false)
    private int distanceKm;

    public TrainRoute() {
    }

    public TrainRoute(String code, String name, String origin, String destination,
                      BigDecimal baseFare, int distanceKm) {
        this.code = code;
        this.name = name;
        this.origin = origin;
        this.destination = destination;
        this.baseFare = baseFare;
        this.distanceKm = distanceKm;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getOrigin() {
        return origin;
    }

    public String getDestination() {
        return destination;
    }

    public BigDecimal getBaseFare() {
        return baseFare;
    }

    public int getDistanceKm() {
        return distanceKm;
    }
}
