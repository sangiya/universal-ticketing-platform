package com.ticketmesh.repository;

import com.ticketmesh.model.TripItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TripItemRepository extends JpaRepository<TripItem, Long> {

    List<TripItem> findByTrip_IdOrderByPosition(Long tripId);
}
