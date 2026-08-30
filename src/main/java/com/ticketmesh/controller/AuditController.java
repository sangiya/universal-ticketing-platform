package com.ticketmesh.controller;

import com.ticketmesh.model.AuditLog;
import com.ticketmesh.service.AuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Immutable audit trail read-back for compliance and accountability.
 */
@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditService auditService;
    private final RequestContext requestContext;

    public AuditController(AuditService auditService, RequestContext requestContext) {
        this.auditService = auditService;
        this.requestContext = requestContext;
    }

    @GetMapping
    public ResponseEntity<List<AuditLog>> list(
            @RequestParam(value = "tenantId", required = false) Long tenantId,
            @RequestParam(value = "limit", defaultValue = "50") int limit) {
        return ResponseEntity.ok(auditService.listByTenant(requestContext.tenantIdOr(tenantId), limit));
    }
}