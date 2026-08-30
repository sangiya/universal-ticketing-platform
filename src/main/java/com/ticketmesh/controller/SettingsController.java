package com.ticketmesh.controller;

import com.ticketmesh.dto.UserSettingsRequest;
import com.ticketmesh.model.UserSettings;
import com.ticketmesh.service.UserSettingsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final UserSettingsService settingsService;
    private final RequestContext requestContext;

    public SettingsController(UserSettingsService settingsService,
                              RequestContext requestContext) {
        this.settingsService = settingsService;
        this.requestContext = requestContext;
    }

    @GetMapping
    public ResponseEntity<UserSettings> get() {
        return ResponseEntity.ok(settingsService.getOrCreate(requestContext.currentUserId()));
    }

    @PutMapping
    public ResponseEntity<UserSettings> update(@Valid @RequestBody UserSettingsRequest request) {
        return ResponseEntity.ok(settingsService.update(
                requestContext.currentUserId(),
                request.getTheme(),
                request.getLanguage(),
                request.getCurrency(),
                request.getNotifyEmail(),
                request.getNotifySms(),
                request.getNotifyPush(),
                request.getNotifyWhatsapp()));
    }
}
