package com.ticketmesh.ai.model;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic, self-contained embedding model used when no LLM provider is
 * configured. It hashes character n-grams of the input into a fixed-size
 * vector so that semantically similar text produces nearby vectors (via cosine
 * similarity). This keeps retrieval grounded and fully offline/testable while
 * remaining swappable for a real provider through the {@code ChatModel}/
 * {@code EmbeddingModel} interface.
 */
public class OfflineEmbeddingModel implements EmbeddingModel {

    private static final int DIMENSIONS = 256;

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<String> instructions = request.getInstructions();
        List<Embedding> embeddings = new ArrayList<>(instructions.size());
        for (int i = 0; i < instructions.size(); i++) {
            embeddings.add(new Embedding(embed(instructions.get(i)), i));
        }
        return new EmbeddingResponse(embeddings);
    }

    @Override
    public float[] embed(Document document) {
        return embed(document.getText());
    }

    public float[] embed(String text) {
        float[] vector = new float[DIMENSIONS];
        String normalized = normalize(text);
        if (normalized.isEmpty()) {
            return vector;
        }
        for (int n = 2; n <= 3; n++) {
            for (int i = 0; i + n <= normalized.length(); i++) {
                String gram = normalized.substring(i, i + n);
                int bucket = Math.floorMod(djb2(gram), DIMENSIONS);
                vector[bucket] += 1.0f;
            }
        }
        return normalizeVector(vector);
    }

    private static String normalize(String text) {
        return text == null ? "" : text.toLowerCase().replaceAll("[^a-z0-9 ]", " ").trim();
    }

    private static long djb2(String s) {
        long hash = 5381;
        for (int i = 0; i < s.length(); i++) {
            hash = ((hash << 5) + hash) + s.charAt(i);
        }
        return hash;
    }

    private static float[] normalizeVector(float[] vector) {
        double magnitude = 0.0;
        for (float v : vector) {
            magnitude += (double) v * v;
        }
        if (magnitude == 0.0) {
            return vector;
        }
        double norm = Math.sqrt(magnitude);
        for (int i = 0; i < vector.length; i++) {
            vector[i] = (float) (vector[i] / norm);
        }
        return vector;
    }
}
