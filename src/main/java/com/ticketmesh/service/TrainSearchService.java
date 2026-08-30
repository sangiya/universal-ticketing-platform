package com.ticketmesh.service;

import com.ticketmesh.dto.ScheduleResponse;
import com.ticketmesh.model.TrainSchedule;
import com.ticketmesh.repository.TrainScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class TrainSearchService {

    private final TrainScheduleRepository scheduleRepository;

    public TrainSearchService(TrainScheduleRepository scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponse> search(LocalDate serviceDate, String origin, String destination) {
        if (serviceDate == null) {
            serviceDate = LocalDate.now();
        }
        boolean hasOrigin = origin != null && !origin.isBlank();
        boolean hasDestination = destination != null && !destination.isBlank();

        List<TrainSchedule> schedules;
        if (hasOrigin && hasDestination) {
            schedules = scheduleRepository
                    .findByServiceDateAndRoute_OriginAndRoute_Destination(
                            serviceDate, origin.trim(), destination.trim());
        } else {
            schedules = scheduleRepository.findByServiceDate(serviceDate);
        }

        return schedules.stream()
                .filter(s -> s.getAvailableSeats() > 0)
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ScheduleResponse findById(Long id) {
        TrainSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new com.ticketmesh.exception.NotFoundException(
                        "Schedule not found: " + id));
        return toResponse(schedule);
    }

    private ScheduleResponse toResponse(TrainSchedule s) {
        return new ScheduleResponse(
                s.getId(),
                s.getTrainCode(),
                s.getRoute().getCode(),
                s.getRoute().getName(),
                s.getRoute().getOrigin(),
                s.getRoute().getDestination(),
                s.getServiceDate(),
                s.getDepartureTime(),
                s.getArrivalTime(),
                s.getCapacity(),
                s.getAvailableSeats(),
                s.getFare());
    }
}
