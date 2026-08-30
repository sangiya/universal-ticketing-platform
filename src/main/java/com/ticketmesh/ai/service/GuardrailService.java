package com.ticketmesh.ai.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Applies content-safety controls to both the inbound user prompt and the
 * outbound assistant reply: PII redaction, prompt-injection detection and
 * toxicity screening.
 */
@Service
public class GuardrailService {

    private static final String EMAIL = "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}";
    private static final String PHONE = "(\\+\\d{1,3}[\\s-]?)?\\(?\\d{3}\\)?[\\s.-]?\\d{3}[\\s.-]?\\d{4}";
    private static final String CARD = "\\b\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}\\b";

    private static final List<Pattern> INJECTION_PATTERNS = List.of(
            Pattern.compile("(?i)ignore\\s+(all\\s+)?previous\\s+instructions"),
            Pattern.compile("(?i)(disregard|forget)\\s+(all\\s+)?(prior|previous)\\s+(instructions|prompts)"),
            Pattern.compile("(?i)act\\s+as\\s+if\\s+you\\s+(are|have)\\s+no\\s+(restrictions|guardrails|rules)"),
            Pattern.compile("(?i)you\\s+are\\s+now\\s+.*?(unrestricted|without\\s+rules|developer\\s+mode)"),
            Pattern.compile("(?i)reveal\\s+your\\s+(system\\s+)?prompt"));

    private static final List<Pattern> TOXIC_PATTERNS = List.of(
            Pattern.compile("(?i)\\b(slur\\w*)\\b"),
            Pattern.compile("(?i)\\b(fuck|shit|bitch)\\w*\\b"));

    private enum Outcome {
        ALLOWED,
        INJECTION_BLOCKED,
        TOXIC_BLOCKED
    }

    public String redact(String text) {
        String result = text == null ? "" : text;
        result = result.replaceAll(EMAIL, "[REDACTED-EMAIL]");
        result = result.replaceAll(CARD, "[REDACTED-CARD]");
        result = result.replaceAll(PHONE, "[REDACTED-PHONE]");
        return result;
    }

    public boolean isBlocked(String prompt) {
        return classify(prompt) != Outcome.ALLOWED;
    }

    public BlockReason blockReason(String prompt) {
        return switch (classify(prompt)) {
            case INJECTION_BLOCKED -> BlockReason.PROMPT_INJECTION;
            case TOXIC_BLOCKED -> BlockReason.TOXIC_LANGUAGE;
            default -> BlockReason.NONE;
        };
    }

    private Outcome classify(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return Outcome.ALLOWED;
        }
        for (Pattern p : INJECTION_PATTERNS) {
            if (p.matcher(prompt).find()) {
                return Outcome.INJECTION_BLOCKED;
            }
        }
        for (Pattern p : TOXIC_PATTERNS) {
            if (p.matcher(prompt).find()) {
                return Outcome.TOXIC_BLOCKED;
            }
        }
        return Outcome.ALLOWED;
    }

    public enum BlockReason {
        NONE,
        PROMPT_INJECTION,
        TOXIC_LANGUAGE
    }
}
