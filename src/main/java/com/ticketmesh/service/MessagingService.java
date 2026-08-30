package com.ticketmesh.service;

import com.ticketmesh.dto.ChannelIntegrationResponse;
import com.ticketmesh.dto.MessagingMessageResponse;
import com.ticketmesh.exception.ConflictException;
import com.ticketmesh.exception.NotFoundException;
import com.ticketmesh.integration.MessagingDispatcher;
import com.ticketmesh.model.ChannelIntegration;
import com.ticketmesh.model.MessagingMessage;
import com.ticketmesh.model.Notification;
import com.ticketmesh.model.Tenant;
import com.ticketmesh.repository.ChannelIntegrationRepository;
import com.ticketmesh.repository.MessagingMessageRepository;
import com.ticketmesh.repository.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Omnichannel messaging: inbound webhooks (deduplicated by external ref),
 * outbound replies through the channel adapters, and per-tenant channel
 * integration configuration (WhatsApp / Facebook / Telegram / SMS).
 */
@Service
public class MessagingService {

    private static final Logger log = LoggerFactory.getLogger(MessagingService.class);
    private static final long CHAT_RECIPIENT_ID = 0L;

    private final MessagingMessageRepository messageRepository;
    private final ChannelIntegrationRepository channelIntegrationRepository;
    private final TenantRepository tenantRepository;
    private final NotificationService notificationService;
    private final MessagingDispatcher dispatcher;

    public MessagingService(MessagingMessageRepository messageRepository,
                            ChannelIntegrationRepository channelIntegrationRepository,
                            TenantRepository tenantRepository,
                            NotificationService notificationService,
                            MessagingDispatcher dispatcher) {
        this.messageRepository = messageRepository;
        this.channelIntegrationRepository = channelIntegrationRepository;
        this.tenantRepository = tenantRepository;
        this.notificationService = notificationService;
        this.dispatcher = dispatcher;
    }

    @Transactional
    public MessagingMessageResponse receiveWebhook(Long tenantId, String channel,
                                                   String externalRef, String senderRef,
                                                   String body) {
        Tenant tenant = requireTenant(tenantId);
        MessagingMessage.Channel ch = parseChannel(channel);
        if (externalRef != null && !externalRef.isBlank()) {
            var existing = messageRepository.findByExternalRef(externalRef);
            if (existing.isPresent()) {
                log.info("Webhook {} for tenant {} already processed; skipping", externalRef,
                        tenantId);
                return toResponse(existing.get());
            }
        }
        MessagingMessage message = new MessagingMessage(
                tenant, ch, MessagingMessage.Direction.INBOUND,
                externalRef, senderRef, null, body);
        message.setStatus(MessagingMessage.Status.PROCESSED);
        messageRepository.save(message);
        autoReply(tenant, ch, senderRef, body);
        return toResponse(message);
    }

    @Transactional
    public MessagingMessageResponse reply(Long tenantId, String channel, String recipientRef,
                                          String body) {
        Tenant tenant = requireTenant(tenantId);
        MessagingMessage.Channel ch = parseChannel(channel);
        MessagingMessage message = new MessagingMessage(
                tenant, ch, MessagingMessage.Direction.OUTBOUND,
                null, null, recipientRef, body);
        Notification.Channel notificationChannel = Notification.Channel.valueOf(ch.name());
        try {
            dispatcher.dispatch(message);
            message.setStatus(MessagingMessage.Status.SENT);
            notificationService.notify(tenantId, CHAT_RECIPIENT_ID, notificationChannel,
                    "Outbound " + ch.name() + " message sent", body);
        } catch (RuntimeException ex) {
            log.warn("Failed to dispatch outbound {} message for tenant {}. Reason: {}",
                    ch, tenantId, ex.getMessage());
            message.setStatus(MessagingMessage.Status.FAILED);
            notificationService.notify(tenantId, CHAT_RECIPIENT_ID, notificationChannel,
                    "Outbound " + ch.name() + " message failed", body);
        }
        messageRepository.save(message);
        return toResponse(message);
    }

    @Transactional(readOnly = true)
    public List<MessagingMessageResponse> list(Long tenantId) {
        return messageRepository.findByTenant_IdOrderByCreatedAtDesc(tenantId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public ChannelIntegrationResponse configureChannel(Long tenantId, String channel,
                                                       String name, String apiKeyRef) {
        Tenant tenant = requireTenant(tenantId);
        ChannelIntegration.Channel ch;
        try {
            ch = ChannelIntegration.Channel.valueOf(channel.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ConflictException("Unsupported messaging channel: " + channel
                    + " (expected WHATSAPP, FACEBOOK, TELEGRAM or SMS)");
        }
        ChannelIntegration integration = channelIntegrationRepository
                .findByTenant_IdAndChannel(tenantId, ch)
                .orElse(null);
        if (integration == null) {
            integration = new ChannelIntegration(tenant, ch, name, apiKeyRef);
        } else {
            integration.setName(name);
            integration.setApiKeyRef(apiKeyRef);
            integration.setEnabled(true);
        }
        ChannelIntegration saved = channelIntegrationRepository.save(integration);
        return toResponse(saved);
    }

    private void autoReply(Tenant tenant, MessagingMessage.Channel channel, String senderRef,
                           String body) {
        if (senderRef == null || senderRef.isBlank() || body == null || body.isBlank()) {
            return;
        }
        String normalized = body.toLowerCase();
        if (normalized.contains("hi") || normalized.contains("hello")
                || normalized.contains("price") || normalized.contains("cost")) {
            reply(tenant.getId(), channel.name(), senderRef,
                    "Thanks for reaching TicketMesh! Our team will reply to you shortly.");
        }
    }

    private Tenant requireTenant(Long tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new NotFoundException("Tenant not found: " + tenantId));
    }

    private MessagingMessage.Channel parseChannel(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ConflictException("Messaging channel is required");
        }
        try {
            return MessagingMessage.Channel.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ConflictException("Unsupported messaging channel: " + raw
                    + " (expected WHATSAPP, FACEBOOK, TELEGRAM or SMS)");
        }
    }

    private MessagingMessageResponse toResponse(MessagingMessage m) {
        return new MessagingMessageResponse(
                m.getId(),
                m.getTenant().getId(),
                m.getChannel().name(),
                m.getDirection().name(),
                m.getExternalRef(),
                m.getSenderRef(),
                m.getRecipientRef(),
                m.getBody(),
                m.getStatus().name(),
                m.getCreatedAt());
    }

    private ChannelIntegrationResponse toResponse(ChannelIntegration c) {
        return new ChannelIntegrationResponse(
                c.getId(),
                c.getTenant().getId(),
                c.getChannel().name(),
                c.getName(),
                c.getApiKeyRef(),
                c.isEnabled(),
                c.getCreatedAt());
    }
}