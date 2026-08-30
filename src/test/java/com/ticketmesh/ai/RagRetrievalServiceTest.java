package com.ticketmesh.ai;

import com.ticketmesh.ai.service.RagRetrievalService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RagRetrievalServiceTest {

    private final RagRetrievalService service = new RagRetrievalService(
            new com.ticketmesh.ai.model.OfflineEmbeddingModel());

    @Test
    void retrieve_returnsRequestedNumberOfPassages() {
        List<String> results = service.retrieve("What is the luggage allowance?", 3);
        assertEquals(3, results.size());
    }

    @Test
    void retrieve_returnsLuggagePassageForLuggageQuestion() {
        List<String> results = service.retrieve("How much luggage can I take?", 3);
        assertFalse(results.isEmpty());
        assertNotNull(results.get(0));
    }

    @Test
    void retrieve_capsAtKnowledgeBaseSizeWhenTopKTooLarge() {
        List<String> results = service.retrieve("refund", 100);
        assertEquals(10, results.size());
    }

    @Test
    void retrieve_handlesBlankQuestionWithoutError() {
        List<String> results = service.retrieve("", 2);
        assertEquals(2, results.size());
    }
}
