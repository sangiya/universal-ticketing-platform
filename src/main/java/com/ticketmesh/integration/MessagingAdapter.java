package com.ticketmesh.integration;

import com.ticketmesh.model.MessagingMessage;

/**
 * Outbound channel adapter (WhatsApp / Facebook / Telegram / SMS). Implementations
 * must be offline-capable and never require real credentials in this build.
 */
public interface MessagingAdapter {

    String channel();

    void send(MessagingMessage message);
}