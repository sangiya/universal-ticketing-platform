package com.ticketmesh.service;

import com.ticketmesh.model.AuditLog;
import com.ticketmesh.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * High-security audit trail. Records every sensitive platform action
 * (admin/agent/security operations) for accountability and compliance.
 */
@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long tenantId, String actorUsername, String action,
                       String resourceType, String resourceRef, String detail,
                       String ipAddress) {
        AuditLog log = new AuditLog(tenantId, actorUsername, action, resourceType,
                resourceRef, truncate(detail, 1000), truncate(ipAddress, 45));
        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> listByTenant(Long tenantId, int limit) {
        List<AuditLog> logs = auditLogRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
        return limit > 0 && logs.size() > limit ? logs.subList(0, limit) : logs;
    }

    @Transactional(readOnly = true)
    public List<AuditLog> listByActor(String actorUsername, int limit) {
        List<AuditLog> logs = auditLogRepository.findByActorUsernameOrderByCreatedAtDesc(actorUsername);
        return limit > 0 && logs.size() > limit ? logs.subList(0, limit) : logs;
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
