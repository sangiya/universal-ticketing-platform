package com.ticketmesh.controller;

import com.ticketmesh.service.OtpService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Two-factor authentication endpoints (OTP).
 *
 * <ul>
 *   <li>POST /api/auth/otp/issue — start a challenge, get a challengeId + (in demo mode) the code</li>
 *   <li>POST /api/auth/otp/verify — verify a code, returns ok + reason</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/auth/otp")
public class OtpController {

    private final OtpService otpService;

    public OtpController(OtpService otpService) {
        this.otpService = otpService;
    }

    public record IssueRequest(String username, String purpose) {}
    public record VerifyRequest(String challengeId, String code) {}

    @PostMapping("/issue")
    public ResponseEntity<?> issue(@RequestBody(required = false) IssueRequest request) {
        try {
            String username = request == null ? null : request.username();
            String purpose = request == null ? "login" : request.purpose();
            return ResponseEntity.ok(otpService.issue(username, purpose));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of(
                    "ok", false,
                    "message", ex.getMessage()
            ));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verify(@RequestBody VerifyRequest request) {
        if (request == null || request.challengeId() == null || request.code() == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "ok", false,
                    "message", "challengeId and code are required"
            ));
        }
        return ResponseEntity.ok(otpService.verify(request.challengeId(), request.code()));
    }
}
