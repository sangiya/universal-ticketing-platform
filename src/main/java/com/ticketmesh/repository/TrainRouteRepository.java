package com.ticketmesh.repository;

import com.ticketmesh.model.TrainRoute;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrainRouteRepository extends JpaRepository<TrainRoute, Long> {
}
