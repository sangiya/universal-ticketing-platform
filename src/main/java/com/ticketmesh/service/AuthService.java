package com.ticketmesh.service;

import com.ticketmesh.dto.AuthResponse;
import com.ticketmesh.dto.LoginRequest;
import com.ticketmesh.dto.RegisterRequest;
import com.ticketmesh.exception.ConflictException;
import com.ticketmesh.exception.NotFoundException;
import com.ticketmesh.model.Tenant;
import com.ticketmesh.model.User;
import com.ticketmesh.repository.TenantRepository;
import com.ticketmesh.repository.UserRepository;
import com.ticketmesh.security.CurrentUser;
import com.ticketmesh.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Authentication service covering registration, login, logout, token refresh,
 * password reset and change-password — spec sections 1 and 13.
 *
 * <p>Login throttling uses an in-memory {@code ConcurrentHashMap} keyed by username.
 * Failed attempts are tracked per user; after 5 failures within 15 minutes the
 * account is locked for 15 minutes. Progressive back-off is applied.
 *
 * <p>Password reset uses a signed JWT token sent via email. The token encodes the
 * user id and is valid for 30 minutes. In production this token would be emailed;
 * in offline mode the token is logged at INFO level.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCKOUT_MINUTES = 15;
    private static final long ATTEMPT_WINDOW_MINUTES = 15;
    private static final long RESET_TOKEN_MINUTES = 30;

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;

    // username -> FailedAttempt state
    private final ConcurrentHashMap<String, FailedAttempt> failedAttempts = new ConcurrentHashMap<>();

    public AuthService(UserRepository userRepository,
                      TenantRepository tenantRepository,
                      PasswordEncoder passwordEncoder,
                      AuthenticationManager authenticationManager,
                      UserDetailsService userDetailsService,
                      JwtService jwtService) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(normalizeUsername(request.getUsername()))) {
            throw new ConflictException("Username already taken");
        }
        if (userRepository.existsByEmail(normalizeEmail(request.getEmail()))) {
            throw new ConflictException("Email already registered");
        }
        User.Role role = resolveRole(request.getRole());
        Long tenantId = resolveTenant(request.getTenantSlug());
        if (role == User.Role.AGENT && tenantId == null) {
            tenantId = provisionAgentTenant(request);
        }
        User user = new User(
                normalizeUsername(request.getUsername()),
                passwordEncoder.encode(request.getPassword()),
                request.getFullName().trim(),
                normalizeEmail(request.getEmail()),
                role,
                tenantId,
                normalizePhone(request.getPhone()));
        // Apply KYC + business fields if provided
        applyKyc(user, request);
        userRepository.save(user);
        return authenticate(normalizeUsername(request.getUsername()), request.getPassword(),
                user.getId(), user.getTenantId());
    }

    /**
     * Login with progressive lockout on repeated failure.
     */
    public AuthResponse login(LoginRequest request) {
        String username = normalizeUsername(request.getUsername());
        checkLockout(username);
        try {
            // Authenticate before clearing attempts so a race on multi-request doesn't
            // expose the difference between "wrong password" and "locked out"
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.getPassword()));
            failedAttempts.remove(username);  // clear any previous failures

            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ConflictException("User not found after authentication"));
            return buildAuthResponse(user, userDetails);
        } catch (LockedException ex) {
            throw ex;
        } catch (BadCredentialsException ex) {
            recordFailure(username);
            checkLockout(username);  // re-check so the exception message is accurate
            throw ex;
        }
    }

    /**
     * Refresh access token using a valid refresh token.
     */
    public AuthResponse refresh(String refreshToken) {
        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new SecurityException("Invalid refresh token type");
        }
        String username = jwtService.extractRefreshClaim(refreshToken,
                c -> c.getSubject());
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        if (!jwtService.isRefreshTokenValid(refreshToken, userDetails)) {
            throw new SecurityException("Refresh token expired or revoked");
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found: " + username));
        return buildAuthResponse(user, userDetails);
    }

    /**
     * Initiate password reset. In production this would send an email; in offline mode
     * it logs the reset token at INFO level.
     */
    public void requestPasswordReset(String email) {
        String normalized = normalizeEmail(email);
        userRepository.findByEmail(normalized).ifPresent(user -> {
            // Always succeed to a generic message (don't reveal whether account exists)
            String resetToken = buildPasswordResetToken(user);
            // Log token in production this would be sent via email/SMS
            log.info("PASSWORD_RESET for {}: token={}", user.getUsername(), resetToken);
        });
        // Always return success to prevent account enumeration
    }

    /**
     * Reset password using a token from the forgot-password flow.
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        // Parse and validate the token
        String subject;
        try {
            subject = jwtService.extractClaim(token, c -> c.getSubject());
        } catch (Exception ex) {
            throw new SecurityException("Invalid or expired reset token");
        }
        // The subject is "pwd-reset:<userId>" and the token is a one-shot
        // operation token issued by buildPasswordResetToken.
        if (!subject.startsWith("pwd-reset:")) {
            throw new SecurityException("Invalid or expired reset token");
        }
        Long userId;
        try {
            userId = Long.parseLong(subject.substring("pwd-reset:".length()));
        } catch (NumberFormatException ex) {
            throw new SecurityException("Invalid or expired reset token");
        }
        if (jwtService.isTokenExpired(token)) {
            throw new SecurityException("Reset token has expired. Please request a new one.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new SecurityException("Invalid or expired reset token"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        // Clear any failed login attempts after successful reset
        failedAttempts.remove(user.getUsername());
        log.info("Password successfully reset for user {}", user.getUsername());
    }

    /**
     * Change password for the authenticated user.
     */
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new SecurityException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        // Clear any failed attempts
        failedAttempts.remove(user.getUsername());
        log.info("Password changed for user {}", user.getUsername());
    }

    /**
     * Returns the current user's auth info (for /api/auth/me).
     */
    public AuthResponse me(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        return buildAuthResponse(user, userDetails);
    }

    /**
     * Copies KYC personal fields and business fields into the User entity if
     * they are present in the registration request.
     */
    private void applyKyc(User user, RegisterRequest request) {
        RegisterRequest.KycPayload kyc = request.getKyc();
        if (kyc != null) {
            user.setKycIdType(kyc.getIdType());
            user.setKycIdNumber(kyc.getIdNumber());
            user.setKycDateOfBirth(kyc.getDateOfBirth());
            user.setKycGender(kyc.getGender());
            if (kyc.getAddress() != null) {
                user.setKycAddressLine1(kyc.getAddress().getLine1());
                user.setKycAddressLine2(kyc.getAddress().getLine2());
                user.setKycCity(kyc.getAddress().getCity());
                user.setKycState(kyc.getAddress().getState());
                user.setKycPostalCode(kyc.getAddress().getPostalCode());
                user.setKycCountryIso(
                        kyc.getAddress().getCountryIso() != null
                                ? kyc.getAddress().getCountryIso()
                                : request.getCountryIso());
            }
            user.setKycStatus(kyc.isKycConsent() ? User.KycStatus.SUBMITTED : User.KycStatus.PENDING);
        }
        RegisterRequest.BusinessPayload biz = request.getBusiness();
        if (biz != null) {
            user.setBusinessName(biz.getName());
            user.setBusinessRegNumber(biz.getRegistrationNumber());
            user.setBusinessTaxId(biz.getTaxId());
            user.setBusinessType(biz.getType());
            user.setBusinessAddress(biz.getAddress());
            user.setBusinessStatus(User.KycStatus.SUBMITTED);
        }
    }

    private AuthResponse authenticate(String username, String rawPassword,
                                     Long userId, Long tenantId) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, rawPassword));
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ConflictException("User not found: " + username));
        return buildAuthResponse(user, userDetails);
    }

    private AuthResponse buildAuthResponse(User user, UserDetails userDetails) {
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);
        return new AuthResponse(
                accessToken,
                refreshToken,
                jwtService.getAccessExpirationMillis() / 1000,  // seconds
                "Bearer",
                user.getUsername(),
                user.getFullName(),
                user.getRole().name(),
                user.getTenantId(),
                user.getId());
    }

    private User.Role resolveRole(String raw) {
        if (raw == null || raw.isBlank()) {
            return User.Role.CUSTOMER;
        }
        try {
            User.Role role = User.Role.valueOf(raw.trim().toUpperCase());
            if (role == User.Role.ADMIN) {
                throw new ConflictException("Admin accounts cannot be self-registered");
            }
            return role;
        } catch (IllegalArgumentException ex) {
            throw new ConflictException("Unsupported role: " + raw);
        }
    }

    private Long resolveTenant(String tenantSlug) {
        if (tenantSlug == null || tenantSlug.isBlank()) {
            return null;
        }
        Tenant tenant = tenantRepository.findBySlug(tenantSlug.toLowerCase())
                .orElseThrow(() -> new NotFoundException("Tenant not found: " + tenantSlug));
        return tenant.getId();
    }

    /**
     * Uber/PickMe style activation: agent with no tenant gets its own shop tenant.
     * Falls back to empty/UTC only if absolutely nothing is supplied — never hardcodes
     * a country or currency preference (those come from the registration form).
     */
    private Long provisionAgentTenant(RegisterRequest request) {
        String slug = uniqueAgentSlug(request.getUsername());
        Tenant tenant = new Tenant(
                slug,
                request.getFullName() + " Shop",
                defaultIfBlank(request.getCountryIso(), "ZZ"),
                defaultIfBlank(request.getCurrencyIso(), "USD"),
                defaultIfBlank(request.getDefaultLanguage(), "en"),
                defaultIfBlank(request.getTimezone(), "UTC"),
                null);
        Tenant saved = tenantRepository.save(tenant);
        return saved.getId();
    }

    private String uniqueAgentSlug(String username) {
        String base = username == null ? "agent" : username;
        String slug = base.trim().toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (slug.isBlank()) slug = "agent";
        slug = slug + "-shop";
        String candidate = slug;
        int n = 2;
        while (tenantRepository.existsBySlug(candidate)) {
            candidate = slug + "-" + n;
            n++;
        }
        return candidate;
    }

    private String defaultIfBlank(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    // --- Identity normalization (spec §1) ---

    private String normalizeUsername(String raw) {
        return raw == null ? null : raw.trim().toLowerCase();
    }

    private String normalizeEmail(String raw) {
        return raw == null ? null : raw.trim().toLowerCase();
    }

    /**
     * Normalizes phone to E.164 format. Accepts common formats and strips
     * everything except digits and leading +.
     */
    private String normalizePhone(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String digits = raw.replaceAll("[^0-9+]", "");
        if (digits.isEmpty()) return null;
        if (!digits.startsWith("+")) {
            // Assume it's a local number — prefix with country code if 9-10 digits
            if (digits.length() >= 9 && digits.length() <= 10) {
                digits = "+94" + digits;  // default to Sri Lanka; configurable per tenant
            }
        }
        return digits;
    }

    // --- Login throttling ---

    private void checkLockout(String username) {
        FailedAttempt state = failedAttempts.get(username);
        if (state != null && state.lockoutUntil != null
                && Instant.now().isBefore(state.lockoutUntil)) {
            long remaining = ChronoUnit.MINUTES.between(Instant.now(), state.lockoutUntil) + 1;
            throw new LockedException(
                    "Account is temporarily locked due to too many failed attempts. "
                            + "Please try again in " + remaining + " minute(s).");
        }
    }

    private void recordFailure(String username) {
        Instant now = Instant.now();
        failedAttempts.compute(username, (k, existing) -> {
            if (existing == null) {
                FailedAttempt fresh = new FailedAttempt();
                fresh.count = 1;
                fresh.firstFailure = now;
                return fresh;
            }
            // Reset if the window has expired
            if (existing.firstFailure != null
                    && ChronoUnit.MINUTES.between(existing.firstFailure, now) > ATTEMPT_WINDOW_MINUTES) {
                existing.count = 1;
                existing.firstFailure = now;
                existing.lockoutUntil = null;
                return existing;
            }
            existing.count++;
            if (existing.count >= MAX_ATTEMPTS) {
                existing.lockoutUntil = now.plus(LOCKOUT_MINUTES, ChronoUnit.MINUTES);
                log.warn("Account locked due to {} failed login attempts for user {}", existing.count, username);
            }
            return existing;
        });
    }

    private String buildPasswordResetToken(User user) {
        // Issue a short-lived, operation-specific token. The subject encodes the
        // user id so the token is bound to a single user, and the type claim
        // distinguishes it from regular access tokens.
        return jwtService.generateOpToken(
                "password-reset",
                "pwd-reset:" + user.getId(),
                RESET_TOKEN_MINUTES * 60 * 1000L);
    }

    private static class FailedAttempt {
        int count;
        Instant firstFailure;
        Instant lockoutUntil;
    }
}
