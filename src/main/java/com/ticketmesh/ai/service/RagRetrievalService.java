package com.ticketmesh.ai.service;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Retrieval service that embeds a small curated travel knowledge base and
 * returns the passages most relevant to a user question by cosine similarity.
 * The embedding store is built in memory on startup so it stays fully offline.
 */
@Service
public class RagRetrievalService {

    private record Passage(String text, float[] vector) {
    }

    private static final List<String> KNOWLEDGE_BASE = List.of(
            "Refunds are processed back to the original payment method within 5-7 business days after a booking is cancelled.",
            "Cancellations made more than 24 hours before departure are free; later cancellations incur a 15% fee.",
            "Each traveller may carry one item of hand luggage plus two checked bags of up to 20kg each.",
            "Boarding closes 5 minutes before scheduled departure. Present your QR ticket at the platform gate.",
            "If your train is delayed by more than 30 minutes you may request a full refund or free rebooking.",
            "Open tickets are valid for any departure on the same route within 30 days of purchase.",
            "Group bookings of 10 or more travellers receive a 10% discount when purchased in a single transaction.",
            "Children under 4 travel free; children aged 4-12 receive a 40% discount on standard fares.",
            "Seat reservations are included for first class and available as an add-on for standard class.",
            "Tickets are issued as a scannable QR code delivered immediately after payment confirmation.");

    private final EmbeddingModel embeddingModel;
    private final List<Passage> passages = new ArrayList<>();

    public RagRetrievalService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
        for (String text : KNOWLEDGE_BASE) {
            passages.add(new Passage(text, embeddingModel.embed(text)));
        }
    }

    public List<String> retrieve(String question, int topK) {
        float[] query = embeddingModel.embed(question == null ? "" : question);
        return passages.stream()
                .sorted(Comparator.comparingDouble(p -> -cosineSimilarity(query, p.vector())))
                .limit(topK)
                .map(Passage::text)
                .toList();
    }

    private static double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) {
            return 0.0;
        }
        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
