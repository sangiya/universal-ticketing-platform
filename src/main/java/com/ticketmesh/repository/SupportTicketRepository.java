package com.ticketmesh.repository;

import com.ticketmesh.model.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    List<SupportTicket> findByRequester_IdOrderByCreatedAtDesc(Long requesterId);

    List<SupportTicket> findByTenant_IdOrderByCreatedAtDesc(Long tenantId);

    List<SupportTicket> findByStatusOrderByCreatedAtDesc(SupportTicket.Status status);

    List<SupportTicket> findByEscalatedTrueOrderByCreatedAtDesc();

    List<SupportTicket> findBySlaDueAtBeforeAndStatusNotIn(
            Instant now, List<SupportTicket.Status> closedStatuses);

    long countByStatus(SupportTicket.Status status);
}
