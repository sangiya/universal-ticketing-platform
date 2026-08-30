package com.ticketmesh.repository;

import com.ticketmesh.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByTenantIdOrderByCreatedAtDesc(Long tenantId);

    List<AuditLog> findByActorUsernameOrderByCreatedAtDesc(String actorUsername);
}
