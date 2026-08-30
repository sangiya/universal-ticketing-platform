package com.ticketmesh.repository;

import com.ticketmesh.model.SupportMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupportMessageRepository extends JpaRepository<SupportMessage, Long> {

    List<SupportMessage> findByTicket_IdOrderByCreatedAtAsc(Long ticketId);
}
