package com.ticketmesh.controller;

import com.ticketmesh.exception.NotFoundException;
import com.ticketmesh.model.User;
import com.ticketmesh.repository.UserRepository;
import com.ticketmesh.security.CurrentUser;
import org.springframework.stereotype.Component;

/**
 * Resolves the acting authenticated user and its default tenant. Users without
 * a tenant fall back to the seeded global tenant id (1) so the platform stays
 * usable for global / non-tenant-scoped accounts.
 */
@Component
public class RequestContext {

    private static final Long DEFAULT_TENANT_ID = 1L;

    private final CurrentUser currentUser;
    private final UserRepository userRepository;

    public RequestContext(CurrentUser currentUser, UserRepository userRepository) {
        this.currentUser = currentUser;
        this.userRepository = userRepository;
    }

    public User currentUser() {
        return userRepository.findByUsername(currentUser.username())
                .orElseThrow(() -> new NotFoundException("Authenticated user not found"));
    }

    public Long currentUserId() {
        return currentUser().getId();
    }

    public Long currentTenantId() {
        Long tenantId = currentUser().getTenantId();
        return tenantId != null ? tenantId : DEFAULT_TENANT_ID;
    }

    public Long tenantIdOr(Long requested) {
        return requested != null ? requested : currentTenantId();
    }
}