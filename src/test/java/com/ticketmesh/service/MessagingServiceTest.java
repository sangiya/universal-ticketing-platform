package com.ticketmesh.service;

import com.ticketmesh.dto.MessagingMessageResponse;
import com.ticketmesh.integration.MessagingDispatcher;
import com.ticketmesh.model.MessagingMessage;
import com.ticketmesh.model.Notification;
import com.ticketmesh.model.Tenant;
import com.ticketmesh.repository.ChannelIntegrationRepository;
import com.ticketmesh.repository.MessagingMessageRepository;
import com.ticketmesh.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MessagingServiceTest {

    private MessagingMessageRepository messageRepository;
    private ChannelIntegrationRepository channelRepository;
    private TenantRepository tenantRepository;
    private NotificationService notificationService;
    private MessagingDispatcher dispatcher;
    private MessagingService messagingService;

    private Tenant tenant;

    @BeforeEach
    void setUp() {
        messageRepository = mock(MessagingMessageRepository.class);
        channelRepository = mock(ChannelIntegrationRepository.class);
        tenantRepository = mock(TenantRepository.class);
        notificationService = mock(NotificationService.class);
        dispatcher = mock(MessagingDispatcher.class);
        messagingService = new MessagingService(
                messageRepository, channelRepository, tenantRepository,
                notificationService, dispatcher);

        tenant = new Tenant("wa-tenant", "WA Tenant", "LK", "LKR", "en",
                "Asia/Colombo", null);
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
        when(messageRepository.save(any(MessagingMessage.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void webhook_inboundCreatesProcessedMessage() {
        MessagingMessageResponse response = messagingService.receiveWebhook(
                1L, "WHATSAPP", "ext-abc", "wa:customer", "How much for a ticket?");

        assertEquals("INBOUND", response.direction());
        assertEquals("PROCESSED", response.status());
        assertEquals("ext-abc", response.externalRef());
        assertEquals("WHATSAPP", response.channel());
        verify(messageRepository).save(any(MessagingMessage.class));
    }

    @Test
    void webhook_duplicateExternalRefReturnsExistingWithoutResave() {
        MessagingMessage existing = new MessagingMessage(tenant,
                MessagingMessage.Channel.WHATSAPP, MessagingMessage.Direction.INBOUND,
                "ext-dup", "wa:customer", null, "Hello");
        existing.setStatus(MessagingMessage.Status.PROCESSED);
        when(messageRepository.findByExternalRef("ext-dup")).thenReturn(Optional.of(existing));

        MessagingMessageResponse response = messagingService.receiveWebhook(
                1L, "WHATSAPP", "ext-dup", "wa:customer", "Hello again");

        assertEquals("Hello", response.body());
        assertEquals("PROCESSED", response.status());
        verify(messageRepository, never()).save(any(MessagingMessage.class));
    }

    @Test
    void reply_marksSentAndEmitsNotification() {
        MessagingMessageResponse response = messagingService.reply(
                1L, "WHATSAPP", "wa:customer", "Your ticket is confirmed");

        assertEquals("OUTBOUND", response.direction());
        assertEquals("SENT", response.status());
        assertEquals("wa:customer", response.recipientRef());
        verify(dispatcher).dispatch(any(MessagingMessage.class));
        verify(notificationService).notify(eq(1L), anyLong(),
                eq(Notification.Channel.WHATSAPP), anyString(), anyString());
    }

    @Test
    void reply_marksFailedWhenDispatcherThrows() {
        org.mockito.Mockito.doThrow(new IllegalStateException("provider down"))
                .when(dispatcher).dispatch(any(MessagingMessage.class));

        MessagingMessageResponse response = messagingService.reply(
                1L, "TELEGRAM", "tg:user", "Hello");

        assertEquals("FAILED", response.status());
        verify(messageRepository).save(any(MessagingMessage.class));
    }

    @Test
    void list_returnsTenantMessagesNewestFirst() {
        MessagingMessage inbound = new MessagingMessage(tenant,
                MessagingMessage.Channel.SMS, MessagingMessage.Direction.INBOUND,
                null, "sms:user", null, "Hi");
        MessagingMessage outbound = new MessagingMessage(tenant,
                MessagingMessage.Channel.FACEBOOK, MessagingMessage.Direction.OUTBOUND,
                null, null, "fb:user", "Hello");
        when(messageRepository.findByTenant_IdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(inbound, outbound));

        List<MessagingMessageResponse> messages = messagingService.list(1L);

        assertEquals(2, messages.size());
        assertEquals("INBOUND", messages.get(0).direction());
        assertEquals("OUTBOUND", messages.get(1).direction());
    }
}