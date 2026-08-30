package com.ticketmesh.controller;

import com.ticketmesh.model.Notification;
import com.ticketmesh.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * In-app notification inbox for the authenticated customer.
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final RequestContext requestContext;

    public NotificationController(NotificationService notificationService,
                                  RequestContext requestContext) {
        this.notificationService = notificationService;
        this.requestContext = requestContext;
    }

    @GetMapping
    public ResponseEntity<List<Notification>> mine() {
        return ResponseEntity.ok(notificationService.mine(requestContext.currentUserId()));
    }
}