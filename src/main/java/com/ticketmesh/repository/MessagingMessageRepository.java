package com.ticketmesh.repository;

import com.ticketmesh.model.MessagingMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MessagingMessageRepository extends JpaRepository<MessagingMessage, Long> {

    List<MessagingMessage> findByTenant_IdOrderByCreatedAtDesc(Long tenantId);

    Optional<MessagingMessage> findByExternalRef(String externalRef);
}
