package com.ticketmesh.controller;

import com.ticketmesh.dto.IdentityRequest;
import com.ticketmesh.dto.IdentityResponse;
import com.ticketmesh.service.IdentityVerificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class IdentityController {

    private final IdentityVerificationService identityService;
    private final RequestContext requestContext;

    public IdentityController(IdentityVerificationService identityService,
                              RequestContext requestContext) {
        this.identityService = identityService;
        this.requestContext = requestContext;
    }

    @PostMapping("/identity/verify")
    public ResponseEntity<IdentityResponse> verify(@Valid @RequestBody IdentityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(identityService.submit(
                requestContext.currentUserId(),
                request.getDocumentType(),
                request.getDocumentNumber(),
                request.getDocumentPhotoUrl()));
    }

    @GetMapping("/identity/me")
    public ResponseEntity<IdentityResponse> me() {
        return ResponseEntity.ok(identityService.byUser(requestContext.currentUserId()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/identity/{id}/review")
    public ResponseEntity<IdentityResponse> review(
            @PathVariable("id") Long id,
            @RequestParam("approve") boolean approve) {
        return ResponseEntity.ok(identityService.review(
                id, approve, requestContext.currentUserId()));
    }
}