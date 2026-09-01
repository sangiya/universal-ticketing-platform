package com.ticketmesh.dto;

/**
 * Authentication response containing both access and refresh tokens, following OAuth2/OIDC
 * compatible flows as required by spec §13.
 *
 * @param accessToken  JWT access token (short-lived, default 24h)
 * @param refreshToken JWT refresh token (long-lived, default 7d) — null for admin/system callers
 * @param expiresIn    Access token TTL in seconds
 * @param tokenType    Always "Bearer"
 * @param username     Authenticated username
 * @param fullName     User's display name
 * @param role         Role name: CUSTOMER, AGENT or ADMIN
 * @param tenantId     Associated tenant ID, or null for platform admins
 * @param userId       User's unique ID
 */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        String tokenType,
        String username,
        String fullName,
        String role,
        Long tenantId,
        Long userId
) {
    public AuthResponse(String accessToken, String refreshToken, long expiresIn,
                       String tokenType, String username, String fullName, String role) {
        this(accessToken, refreshToken, expiresIn, tokenType, username, fullName, role, null, null);
    }
}
