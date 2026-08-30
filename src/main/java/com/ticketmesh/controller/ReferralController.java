package com.ticketmesh.controller;

import com.ticketmesh.dto.ReferralInviteRequest;
import com.ticketmesh.dto.ReferralResponse;
import com.ticketmesh.service.ReferralService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/referrals")
public class ReferralController {

    private final ReferralService referralService;
    private final RequestContext requestContext;

    public ReferralController(ReferralService referralService,
                              RequestContext requestContext) {
        this.referralService = referralService;
        this.requestContext = requestContext;
    }

    @PostMapping
    public ResponseEntity<ReferralResponse> createMyCode() {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                referralService.create(requestContext.currentUserId()));
    }

    @PostMapping("/invite")
    public ResponseEntity<ReferralResponse> invite(
            @Valid @RequestBody ReferralInviteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                referralService.invite(requestContext.currentUserId(), request.getEmail()));
    }

    @GetMapping
    public ResponseEntity<List<ReferralResponse>> myCodes() {
        return ResponseEntity.ok(referralService.myCodes(requestContext.currentUserId()));
    }

    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validate(
            @RequestParam("code") String code) {
        boolean valid = referralService.validate(code);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("valid", valid);
        return ResponseEntity.ok(body);
    }
}
