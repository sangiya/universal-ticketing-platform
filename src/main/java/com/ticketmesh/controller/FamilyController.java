package com.ticketmesh.controller;

import com.ticketmesh.dto.FamilyGroupRequest;
import com.ticketmesh.dto.FamilyGroupResponse;
import com.ticketmesh.dto.FamilyMemberResponse;
import com.ticketmesh.service.FamilyGroupService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/family")
public class FamilyController {

    private final FamilyGroupService familyGroupService;
    private final RequestContext requestContext;

    public FamilyController(FamilyGroupService familyGroupService,
                            RequestContext requestContext) {
        this.familyGroupService = familyGroupService;
        this.requestContext = requestContext;
    }

    @PostMapping
    public ResponseEntity<FamilyGroupResponse> create(@Valid @RequestBody FamilyGroupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(familyGroupService.create(
                requestContext.currentUserId(),
                requestContext.currentTenantId(),
                request.getName()));
    }

    @PostMapping("/{familyId}/join")
    public ResponseEntity<FamilyGroupResponse> join(@PathVariable("familyId") Long familyId) {
        return ResponseEntity.ok(familyGroupService.join(
                requestContext.currentUserId(),
                requestContext.currentTenantId(),
                familyId));
    }

    @DeleteMapping("/{familyId}/members/{userId}")
    public ResponseEntity<FamilyGroupResponse> removeMember(
            @PathVariable("familyId") Long familyId,
            @PathVariable("userId") Long memberUserId) {
        return ResponseEntity.ok(familyGroupService.removeMember(
                familyId, memberUserId, requestContext.currentUserId()));
    }

    @GetMapping
    public ResponseEntity<List<FamilyGroupResponse>> myGroups() {
        return ResponseEntity.ok(familyGroupService.myGroups(requestContext.currentUserId()));
    }

    @GetMapping("/{familyId}/members")
    public ResponseEntity<List<FamilyMemberResponse>> members(
            @PathVariable("familyId") Long familyId) {
        return ResponseEntity.ok(familyGroupService.members(
                familyId, requestContext.currentUserId()));
    }
}
