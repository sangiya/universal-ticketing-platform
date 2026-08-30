package com.ticketmesh.ai.model;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.Locale;

/**
 * Deterministic chat model used when no LLM provider is configured. It honours
 * a block of retrieved evidence (the {@code CONTEXT:} section injected by the
 * assistant service) and produces a grounded, rule-based answer. The
 * implementation is intentionally stateless and idempotent so offline flows are
 * fully reproducible in tests.
 */
public class OfflineChatModel implements ChatModel {

    private static final String CONTEXT_MARKER = "CONTEXT:";

    @Override
    public ChatResponse call(Prompt prompt) {
        String content = prompt.getContents() == null ? "" : prompt.getContents();
        String question = extractQuestion(content);
        List<String> context = extractContext(content);
        String answer = answer(question, context);
        return ChatResponse.builder()
                .generations(List.of(new Generation(new AssistantMessage(answer))))
                .build();
    }

    private static String extractQuestion(String content) {
        int marker = content.indexOf(CONTEXT_MARKER);
        if (marker >= 0) {
            return content.substring(0, marker).trim();
        }
        return content.trim();
    }

    private static List<String> extractContext(String content) {
        int marker = content.indexOf(CONTEXT_MARKER);
        if (marker < 0) {
            return List.of();
        }
        String rest = content.substring(marker + CONTEXT_MARKER.length()).trim();
        return rest.lines().map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private static String answer(String question, List<String> context) {
        if (!context.isEmpty()) {
            return "Based on available travel information:\n" + String.join("\n", context);
        }
        String q = question == null ? "" : question.toLowerCase(Locale.ROOT);
        if (q.contains("refund")) {
            return "Refunds are processed back to the original payment method within 5-7 business days after a booking is cancelled.";
        }
        if (q.contains("luggage") || q.contains("baggage")) {
            return "Each traveller may carry one item of hand luggage plus two checked bags of up to 20kg each.";
        }
        if (q.contains("boarding")) {
            return "Boarding closes 5 minutes before scheduled departure. Please present your QR ticket at the platform gate.";
        }
        if (q.contains("delay")) {
            return "If your train is delayed by more than 30 minutes you may request a full refund or free rebooking.";
        }
        return "I can help you with tickets, bookings, refunds, luggage, boarding and delays. Ask a specific question so I can give you a precise answer.";
    }
}
