package com.ticketmesh.ai.service;

import com.ticketmesh.ai.dto.AiAssistantResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * End-to-end assistant flow: content-safety screening, retrieval-augmented
 * context injection and generation, then PII redaction of the reply.
 */
@Service
public class AiAssistantService {

    private static final String SYSTEM_PROMPT = """
            You are a helpful travel assistant for a point-to-point rail booking
            service. Answer only using the supplied travel context. If the context
            does not cover the question, recommend asking for clarification.
            Keep answers concise and do not invent pricing, policies or contact
            details that are not present in the context.
            """;

    private final GuardrailService guardrailService;
    private final RagRetrievalService retrievalService;
    private final ChatModel chatModel;

    public AiAssistantService(GuardrailService guardrailService,
                              RagRetrievalService retrievalService,
                              ChatModel chatModel) {
        this.guardrailService = guardrailService;
        this.retrievalService = retrievalService;
        this.chatModel = chatModel;
    }

    public AiAssistantResponse answer(String question) {
        GuardrailService.BlockReason reason = guardrailService.blockReason(question);
        if (reason != GuardrailService.BlockReason.NONE) {
            return new AiAssistantResponse(
                    "Your request was not processed because it was flagged as "
                            + reason.name().toLowerCase().replace('_', ' ') + ".",
                    reason.name(), List.of());
        }

        List<String> sources = retrievalService.retrieve(question, 3);
        Prompt prompt = buildPrompt(question, sources);
        ChatResponse response = chatModel.call(prompt);
        String rawAnswer = response.getResult().getOutput().getText();
        String safeAnswer = guardrailService.redact(rawAnswer);

        return new AiAssistantResponse(safeAnswer, "none", sources);
    }

    private Prompt buildPrompt(String question, List<String> sources) {
        StringBuilder context = new StringBuilder();
        for (String source : sources) {
            context.append("- ").append(source).append('\n');
        }
        String userContent = question + "\nCONTEXT:\n" + context;

        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(SYSTEM_PROMPT));
        messages.add(new UserMessage(userContent));
        return new Prompt(messages);
    }
}
