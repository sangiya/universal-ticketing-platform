package com.ticketmesh.integration;

import com.ticketmesh.model.MessagingMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Routes an outbound message to the adapter matching its channel. Channels
 * without a registered adapter fall back to a no-op that still treats delivery
 * as completed offline.
 */
@Component
public class MessagingDispatcher {

    private static final Logger log = LoggerFactory.getLogger(MessagingDispatcher.class);

    private final List<MessagingAdapter> adapters;

    public MessagingDispatcher(List<MessagingAdapter> adapters) {
        this.adapters = adapters;
    }

    public void dispatch(MessagingMessage message) {
        MessagingAdapter adapter = adapters.stream()
                .filter(a -> a.channel().equalsIgnoreCase(message.getChannel().name()))
                .findFirst()
                .orElse(null);
        if (adapter == null) {
            log.info("No adapter for channel {}; message treated as delivered",
                    message.getChannel());
            return;
        }
        adapter.send(message);
    }
}