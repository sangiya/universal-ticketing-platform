package com.ticketmesh.service;

import com.ticketmesh.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(
            "test-secret-key-that-is-definitely-long-enough-2026",
            3_600_000L,
            "test-refresh-secret-key-that-is-also-long-enough-2026",
            604_800_000L);

    private UserDetails user() {
        return User.withUsername("alice")
                .password("enc")
                .authorities("ROLE_USER")
                .build();
    }

    @Test
    void accessTokenRoundTripsUsername() {
        String token = jwtService.generateAccessToken(user());
        assertNotNull(token);
        assertEquals("alice", jwtService.extractUsername(token));
        assertTrue(jwtService.isAccessToken(token));
        assertFalse(jwtService.isRefreshToken(token));
    }

    @Test
    void refreshTokenRoundTripsUsername() {
        String token = jwtService.generateRefreshToken(user());
        assertNotNull(token);
        assertTrue(jwtService.isRefreshToken(token));
        assertFalse(jwtService.isAccessToken(token));
    }

    @Test
    void accessTokenIsValidForSameUser() {
        String token = jwtService.generateAccessToken(user());
        assertTrue(jwtService.isTokenValid(token, user()));
    }

    @Test
    void accessTokenIsInvalidForDifferentUser() {
        String token = jwtService.generateAccessToken(user());
        UserDetails other = User.withUsername("bob")
                .password("enc").authorities("ROLE_USER").build();
        assertFalse(jwtService.isTokenValid(token, other));
    }

    @Test
    void refreshTokenIsValidForSameUser() {
        String token = jwtService.generateRefreshToken(user());
        assertTrue(jwtService.isRefreshTokenValid(token, user()));
    }

    @Test
    void opTokenIsDetected() {
        String token = jwtService.generateOpToken("password-reset", "pwd-reset:42", 60_000L);
        assertNotNull(token);
        assertFalse(jwtService.isAccessToken(token));
        assertFalse(jwtService.isRefreshToken(token));
        assertEquals("pwd-reset:42", jwtService.extractUsername(token));
    }

    @Test
    void accessExpirationIsReturned() {
        assertEquals(3_600_000L, jwtService.getAccessExpirationMillis());
        assertEquals(604_800_000L, jwtService.getRefreshExpirationMillis());
    }
}
