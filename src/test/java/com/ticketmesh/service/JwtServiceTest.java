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
            "test-secret-key-that-is-definitely-long-enough-2026", 3600000);

    private UserDetails user() {
        return User.withUsername("alice")
                .password("enc")
                .authorities("ROLE_USER")
                .build();
    }

    @Test
    void tokenRoundTripsUsername() {
        String token = jwtService.generateToken(user());
        assertNotNull(token);
        assertEquals("alice", jwtService.extractUsername(token));
    }

    @Test
    void tokenIsValidForSameUser() {
        String token = jwtService.generateToken(user());
        assertTrue(jwtService.isTokenValid(token, user()));
    }

    @Test
    void tokenIsInvalidForDifferentUser() {
        String token = jwtService.generateToken(user());
        UserDetails other = User.withUsername("bob")
                .password("enc").authorities("ROLE_USER").build();
        assertFalse(jwtService.isTokenValid(token, other));
    }
}
