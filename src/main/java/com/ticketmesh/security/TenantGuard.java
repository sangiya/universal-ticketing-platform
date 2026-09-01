package com.ticketmesh.security;

import com.ticketmesh.exception.ConflictException;
import com.ticketmesh.exception.NotFoundException;
import com.ticketmesh.model.User;
import com.ticketmesh.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Tenant isolation guard (spec §4). All tenant-scoped operations must go
 * through {@link #requireAccessTo(Long)} to confirm the calling principal
 * either:
 *
 * <ul>
 *   <li>is a platform administrator (cross-tenant access), or
 *   <li>is a tenant user whose {@code tenantId} matches the requested tenant.
 * </ul>
 *
 * Cross-tenant access is deny-by-default. The guard is intentionally explicit
 * so it appears in code review and audit trails.
 */
@Component
public class TenantGuard {

    private final CurrentUser currentUser;
    private final UserRepository userRepository;

    public TenantGuard(CurrentUser currentUser, UserRepository userRepository) {
        this.currentUser = currentUser;
        this.userRepository = userRepository;
    }

    /**
     * Verify the current principal is allowed to access the requested tenant.
     * Throws if the principal's tenant does not match.
     *
     * @param tenantId the requested tenant id (may be null for global queries)
     * @return the resolved tenant id (falls back to the user's own tenant)
     */
    public Long requireAccessTo(Long tenantId) {
        // Anonymous / unauthenticated requests: skip the per-user check. The
        // SecurityConfig has the matching permitAll() rule; this method is
        // shared by both authenticated and public endpoints.
        if (SecurityContextHolder.getContext().getAuthentication() == null
                || SecurityContextHolder.getContext().getAuthentication()
                        instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
            return tenantId;
        }
        if (isPlatformAdmin()) {
            return tenantId;
        }
        User user = currentUser.user();
        if (tenantId == null) {
            return user.getTenantId();
        }
        if (user.getTenantId() == null) {
            throw new ConflictException("Your account is not associated with a tenant");
        }
        if (!user.getTenantId().equals(tenantId)) {
            throw new NotFoundException("Resource not found");  // 404 to avoid tenant enumeration
        }
        return tenantId;
    }

    /**
     * Asserts that the request's tenant parameter (when provided) matches the
     * caller's tenant. Use this in controller methods that accept an optional
     * {@code tenantId} query param.
     *
     * @param requested tenant id supplied by the client
     * @return the resolved tenant id to use downstream
     */
    public Long requireOrOwned(Long requested) {
        return requireAccessTo(requested);
    }

    public boolean isPlatformAdmin() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        for (GrantedAuthority a : auth.getAuthorities()) {
            String role = a.getAuthority();
            if ("ROLE_ADMIN".equals(role) || "ROLE_PLATFORM_ADMIN".equals(role)) {
                return true;
            }
        }
        return false;
    }
}
