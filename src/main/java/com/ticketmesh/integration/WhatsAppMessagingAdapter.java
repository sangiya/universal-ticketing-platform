package com.ticketmesh.integration;

import com.ticketmesh.model.MessagingMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * WhatsApp outbound adapter. Offline/deterministic: in mock mode (the default
 * unless {@code APP.WHATSAPP_MOCK=false}) messages are logged as delivered.
 * When real dispatch is requested it POSTs to a configured endpoint — no real
 * credentials are ever hardcoded.
 */
@Component
public class WhatsAppMessagingAdapter implements MessagingAdapter {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppMessagingAdapter.class);

    private final boolean enabled;
    private final String endpoint;

    public WhatsAppMessagingAdapter(
            @Value("${app.messaging.whatsapp.enabled:true}") boolean enabled,
            @Value("${app.messaging.whatsapp.endpoint:"
                    + "https://offline.ticketmesh.local/whatsapp/send}") String endpoint) {
        this.enabled = enabled;
        this.endpoint = endpoint;
    }

    @Override
    public String channel() {
        return "WHATSAPP";
    }

    @Override
    public void send(MessagingMessage message) {
        if (!enabled) {
            throw new IllegalStateException("WhatsApp messaging is disabled");
        }
        if (isMockMode()) {
            log.info("WhatsApp mock dispatch to {}: {}", message.getRecipientRef(),
                    message.getBody());
            return;
        }
        httpPost(message);
    }

    private boolean isMockMode() {
        String property = System.getProperty("APP.WHATSAPP_MOCK");
        if (property != null) {
            return !"false".equalsIgnoreCase(property);
        }
        String env = System.getenv("APP.WHATSAPP_MOCK");
        return env == null || !"false".equalsIgnoreCase(env);
    }

    private void httpPost(MessagingMessage message) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(toJson(message),
                            StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "WhatsApp provider responded " + response.statusCode());
            }
        } catch (IOException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("WhatsApp dispatch failed", ex);
        }
    }

    private String toJson(MessagingMessage message) {
        return "{\"id\":\"" + message.getId() + "\",\"channel\":\""
                + message.getChannel() + "\",\"direction\":\""
                + message.getDirection() + "\",\"recipientRef\":\""
                + safe(message.getRecipientRef()) + "\",\"body\":\""
                + safe(message.getBody()) + "\"}";
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}