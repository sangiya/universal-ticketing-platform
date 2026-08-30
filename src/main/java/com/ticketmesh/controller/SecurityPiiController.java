package com.ticketmesh.controller;

import com.ticketmesh.dto.PiiResponse;
import com.ticketmesh.service.PiiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes a masked view of the authenticated user's PII. Raw email / phone /
 * document numbers are never returned; this endpoint only surfaces masked
 * forms used by marketing/preference screens.
 */
@RestController
@RequestMapping("/api/security/pii")
public class SecurityPiiController {

    private final PiiService piiService;
    private final RequestContext requestContext;

    public SecurityPiiController(PiiService piiService, RequestContext requestContext) {
        this.piiService = piiService;
        this.requestContext = requestContext;
    }

    @GetMapping("/me")
    public ResponseEntity<PiiResponse> me() {
        return ResponseEntity.ok(piiService.myPii(requestContext.currentUserId()));
    }
}
