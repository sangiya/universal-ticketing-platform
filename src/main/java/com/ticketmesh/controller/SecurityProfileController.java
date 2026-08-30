package com.ticketmesh.controller;

import com.ticketmesh.dto.AppKeyRequest;
import com.ticketmesh.dto.AppKeyVerifyRequest;
import com.ticketmesh.dto.OtpSendRequest;
import com.ticketmesh.dto.OtpVerifyRequest;
import com.ticketmesh.dto.TwoFactorTokenRequest;
import com.ticketmesh.exception.ConflictException;
import com.ticketmesh.model.OtpCode;
import com.ticketmesh.service.TwoFactorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Two-factor / app-key profile endpoints: email OTP, RFC 6238 TOTP and API
 * app keys. All endpoints require an authenticated session.
 */
@RestController
@RequestMapping("/api/security/profile")
public class SecurityProfileController {

    private final TwoFactorService twoFactorService;

    public SecurityProfileController(TwoFactorService twoFactorService) {
        this.twoFactorService = twoFactorService;
    }

    @PostMapping("/2fa/otp/send")
    public ResponseEntity<Map<String, Object>> sendOtp(@Valid @RequestBody OtpSendRequest request) {
        OtpCode.Purpose purpose = resolvePurpose(request.getPurpose());
        twoFactorService.sendOtp(request.getEmail(), purpose);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("email", request.getEmail());
        body.put("purpose", purpose.name());
        body.put("sent", true);
        return ResponseEntity.ok(body);
    }

    @PostMapping("/2fa/otp/verify")
    public ResponseEntity<Map<String, Object>> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        boolean valid = twoFactorService.verifyRegistrationOtp(request.getEmail(), request.getCode());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("email", request.getEmail());
        body.put("valid", valid);
        return ResponseEntity.ok(body);
    }

    @PostMapping("/2fa/totp/enable")
    public ResponseEntity<Map<String, Object>> enableTotp(@Valid @RequestBody AppKeyRequest request) {
        String secret = twoFactorService.enableTotp(request.getUsername());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("username", request.getUsername());
        body.put("secret", secret);
        body.put("enabled", true);
        return ResponseEntity.ok(body);
    }

    @PostMapping("/2fa/totp/verify")
    public ResponseEntity<Map<String, Object>> verifyTotp(@Valid @RequestBody TwoFactorTokenRequest request) {
        boolean valid = twoFactorService.verifyTotp(request.getUsername(), request.getCode());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("username", request.getUsername());
        body.put("valid", valid);
        return ResponseEntity.ok(body);
    }

    @PostMapping("/appkey/issue")
    public ResponseEntity<Map<String, Object>> issueAppKey(@Valid @RequestBody AppKeyRequest request) {
        String key = twoFactorService.issueAppKey(request.getUsername());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("username", request.getUsername());
        body.put("key", key);
        return ResponseEntity.ok(body);
    }

    @PostMapping("/appkey/verify")
    public ResponseEntity<Map<String, Object>> verifyAppKey(@Valid @RequestBody AppKeyVerifyRequest request) {
        boolean valid = twoFactorService.verifyAppKey(request.getUsername(), request.getKey());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("username", request.getUsername());
        body.put("valid", valid);
        return ResponseEntity.ok(body);
    }

    private OtpCode.Purpose resolvePurpose(String raw) {
        if (raw == null || raw.isBlank()) {
            return OtpCode.Purpose.REGISTRATION;
        }
        try {
            return OtpCode.Purpose.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ConflictException("Unsupported OTP purpose: " + raw
                    + " (expected REGISTRATION or LOGIN_2FA)");
        }
    }
}