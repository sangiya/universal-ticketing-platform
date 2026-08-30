package com.ticketmesh.repository;

import com.ticketmesh.model.TrainSchedule;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TrainScheduleRepository extends JpaRepository<TrainSchedule, Long> {

    List<TrainSchedule> findByServiceDate(LocalDate serviceDate);

    List<TrainSchedule> findByServiceDateAndRoute_OriginAndRoute_Destination(
            LocalDate serviceDate, String origin, String destination);

    Optional<TrainSchedule> findByTrainCodeAndServiceDate(String trainCode, LocalDate serviceDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from TrainSchedule s where s.id = :id")
    Optional<TrainSchedule> findByIdForUpdate(@Param("id") Long id);
}
