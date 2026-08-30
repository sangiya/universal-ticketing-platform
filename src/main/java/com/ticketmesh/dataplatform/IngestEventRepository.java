package com.ticketmesh.dataplatform;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IngestEventRepository extends JpaRepository<IngestEvent, Long> {

    List<IngestEvent> findByProcessingStatusOrderByIdAsc(IngestEvent.ProcessingStatus status);

    long countByEventType(String eventType);
}