package com.ticketmesh.controller;

import com.ticketmesh.dto.ChannelConfigRequest;
import com.ticketmesh.dto.ChannelIntegrationResponse;
import com.ticketmesh.dto.MessagingMessageResponse;
import com.ticketmesh.dto.SendMessageRequest;
import com.ticketmesh.dto.WebhookRequest;
import com.ticketmesh.service.MessagingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Omnichannel messaging endpoints. The webhook is public (that is how Meta /
 * WhatsApp / Facebook call in); the rest require authentication.
 */
@RestController
@RequestMapping("/api/messaging")
public class MessagingController {

    private final MessagingService messagingService;
    private final RequestContext requestContext;

    public MessagingController(MessagingService messagingService,
                               RequestContext requestContext) {
        this.messagingService = messagingService;
        this.requestContext = requestContext;
    }

    @PostMapping("/webhook/tenant/{tenantId}/channel/{channel}")
    public ResponseEntity<MessagingMessageResponse> webhook(
            @PathVariable("tenantId") Long tenantId,
            @PathVariable("channel") String channel,
            @Valid @RequestBody WebhookRequest request) {
        return ResponseEntity.ok(messagingService.receiveWebhook(
                tenantId, channel, request.getExternalRef(),
                request.getSenderRef(), request.getBody()));
    }

    @PostMapping("/send")
    public ResponseEntity<MessagingMessageResponse> send(
            @Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.ok(messagingService.reply(
                requestContext.currentTenantId(),
                request.getChannel(),
                request.getRecipientRef(),
                request.getBody()));
    }

    @GetMapping
    public ResponseEntity<List<MessagingMessageResponse>> mine() {
        return ResponseEntity.ok(messagingService.list(requestContext.currentTenantId()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/channels/{tenantId}")
    public ResponseEntity<ChannelIntegrationResponse> configureChannel(
            @PathVariable("tenantId") Long tenantId,
            @Valid @RequestBody ChannelConfigRequest request) {
        return ResponseEntity.ok(messagingService.configureChannel(
                tenantId, request.getChannel(), request.getName(), request.getApiKeyRef()));
    }
}