package com.ticketmesh.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class CreateBookingRequest {

    @NotNull
    private Long scheduleId;

    @NotNull
    @FutureOrPresent
    private LocalDate travelDate;

    @NotBlank
    @Size(max = 120)
    private String passengerName;

    public Long getScheduleId() {
        return scheduleId;
    }

    public LocalDate getTravelDate() {
        return travelDate;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public void setScheduleId(Long scheduleId) {
        this.scheduleId = scheduleId;
    }

    public void setTravelDate(LocalDate travelDate) {
        this.travelDate = travelDate;
    }

    public void setPassengerName(String passengerName) {
        this.passengerName = passengerName;
    }
}
