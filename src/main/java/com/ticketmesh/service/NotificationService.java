package com.ticketmesh.service;

import com.ticketmesh.model.Notification;
import com.ticketmesh.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Notification service (in-app / email / push) covering all production
 * e-services notifications: booking confirmations, alerts, support updates,
 * promotions. Offline-capable (persists + marks sent).
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public Notification notify(Long tenantId, Long recipientId, Notification.Channel channel,
                               String subject, String body) {
        Notification n = new Notification(tenantId, recipientId, channel, subject, body);
        Notification saved = notificationRepository.save(n);
        // Offline delivery: mark SENT (a real adapter would hand off to email/SMS/push).
        saved.setStatus(Notification.Status.SENT);
        notificationRepository.save(saved);
        log.info("Notified user {} via {}: {}", recipientId, channel, subject);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Notification> mine(Long recipientId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId);
    }
}
