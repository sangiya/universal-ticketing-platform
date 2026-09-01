package com.ticketmesh.controller;

import com.ticketmesh.dto.AuthResponse;
import com.ticketmesh.dto.ChangePasswordRequest;
import com.ticketmesh.dto.ForgotPasswordRequest;
import com.ticketmesh.dto.LoginRequest;
import com.ticketmesh.dto.PasswordResetRequest;
import com.ticketmesh.dto.RefreshTokenRequest;
import com.ticketmesh.dto.RegisterRequest;
import com.ticketmesh.security.CurrentUser;
import com.ticketmesh.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication controller implementing OAuth2/OIDC-compatible flows as required
 * by spec §1 and §13:
 *
 * <ul>
 *   <li>POST /api/auth/register — self-registration (customer/agent, no admin)
 *   <li>POST /api/auth/login — password login with progressive lockout
 *   <li>POST /api/auth/refresh — token refresh (no credentials required)
 *   <li>POST /api/auth/forgot-password — initiate password reset (generic success)
 *   <li>POST /api/auth/reset-password — complete password reset with token
 *   <li>POST /api/auth/change-password — change password for authenticated user
 *   <li>GET  /api/auth/me — return current user info
 * </ul>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final CurrentUser currentUser;

    public AuthController(AuthService authService, CurrentUser currentUser) {
        this.authService = authService;
        this.currentUser = currentUser;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Exchange a valid refresh token for a new access token.
     * No credentials required; caller presents their refresh token.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    /**
     * Initiate password reset. Always returns 200 OK to prevent account enumeration.
     * In production the reset link is emailed; in offline mode it is logged.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.requestPasswordReset(request.email());
        return ResponseEntity.ok().build();
    }

    /**
     * Complete password reset using the token emailed to the user.
     * The token encodes the user ID and is valid for 30 minutes.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        authService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok().build();
    }

    /**
     * Change the authenticated user's password. Requires current password.
     * Uses the current user's ID from the security context.
     */
    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        Long userId = currentUser.userId();
        authService.changePassword(userId, request.currentPassword(), request.newPassword());
        return ResponseEntity.ok().build();
    }

    /**
     * Return the current authenticated user's profile and token info.
     * Useful for session refresh without requiring a full login.
     */
    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me() {
        return ResponseEntity.ok(authService.me(currentUser.username()));
    }
}

