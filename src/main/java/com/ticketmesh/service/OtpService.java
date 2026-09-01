package com.ticketmesh.service;

import com.ticketmesh.model.Notification;
import com.ticketmesh.model.User;
import com.ticketmesh.repository.NotificationRepository;
import com.ticketmesh.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Two-factor authentication using one-time passwords (OTP) delivered via
 * the platform's notification system. Codes are held in-memory with a
 * 5-minute TTL and 5-attempt cap; the API surface is the same whether the
 * backend is in-memory or persisted.
 */
@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long TTL_SECONDS = 5 * 60;
    private static final int MAX_ATTEMPTS = 5;

    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final Map<String, OtpEntry> store = new ConcurrentHashMap<>();

    @Value("${app.otp.demo-mode:true}")
    private boolean demoMode;

    public OtpService(UserRepository userRepository, NotificationRepository notificationRepository) {
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
    }

    public record OtpIssueResponse(String challengeId, String demoCode, int ttlSeconds, String channel) {}

    public record OtpVerifyResponse(boolean ok, String reason) {}

    private record OtpEntry(String code, Instant expiresAt, int attempts, Long userId, String purpose) {}

    /**
     * Issue a 6-digit OTP for the given username (or email). Returns
     * a challenge id the client must echo back to {@link #verify}.
     */
    public OtpIssueResponse issue(String username, String purpose) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username is required");
        }
        String trimmed = username.trim();
        User user = userRepository.findByUsername(trimmed).orElse(null);
        if (user == null) {
            user = userRepository.findByEmailIgnoreCase(trimmed).stream().findFirst().orElse(null);
        }
        if (user == null) {
            throw new IllegalArgumentException("No account matches " + trimmed);
        }

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        String challengeId = java.util.UUID.randomUUID().toString();
        store.put(challengeId, new OtpEntry(code, Instant.now().plusSeconds(TTL_SECONDS), 0,
                user.getId(), purpose == null ? "login" : purpose));

        // Best-effort: write to notifications table so the user sees it in
        // their notification inbox, and log it.
        try {
            Notification n = new Notification(
                    user.getTenantId(),
                    user.getId(),
                    Notification.Channel.EMAIL,
                    "Your TicketMesh verification code",
                    "Your one-time code is " + code + ". It expires in 5 minutes.");
            n.setStatus(Notification.Status.SENT);
            notificationRepository.save(n);
            log.info("Issued OTP for user {} (purpose: {})", user.getUsername(), purpose);
        } catch (RuntimeException ex) {
            log.warn("Could not persist OTP notification", ex);
        }

        return new OtpIssueResponse(challengeId, demoMode ? code : null, (int) TTL_SECONDS, "EMAIL");
    }

    public OtpVerifyResponse verify(String challengeId, String code) {
        if (challengeId == null || code == null) {
            return new OtpVerifyResponse(false, "Missing challenge or code");
        }
        OtpEntry entry = store.get(challengeId);
        if (entry == null) {
            return new OtpVerifyResponse(false, "Challenge not found or already used");
        }
        if (Instant.now().isAfter(entry.expiresAt)) {
            store.remove(challengeId);
            return new OtpVerifyResponse(false, "Code expired");
        }
        if (entry.attempts >= MAX_ATTEMPTS) {
            store.remove(challengeId);
            return new OtpVerifyResponse(false, "Too many attempts");
        }
        if (!entry.code.equals(code.trim())) {
            store.put(challengeId, new OtpEntry(entry.code, entry.expiresAt, entry.attempts + 1,
                    entry.userId, entry.purpose));
            return new OtpVerifyResponse(false, "Incorrect code");
        }
        store.remove(challengeId);
        return new OtpVerifyResponse(true, "ok");
    }
}
