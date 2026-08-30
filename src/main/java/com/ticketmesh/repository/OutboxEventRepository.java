package com.ticketmesh.repository;

import com.ticketmesh.model.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findByStatus(OutboxEvent.Status status);
}
