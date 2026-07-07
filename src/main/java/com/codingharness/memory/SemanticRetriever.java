package com.codingharness.memory;

import java.util.*;

/**
 * Retrieves the most relevant {@link ConversationSummary} records for a
 * given query.
 *
 * <p>Includes a standard {@link #cosineSimilarity(float[], float[])}
 * implementation for use when embedding vectors are available. When
 * embeddings are not present, falls back to keyword-based relevance
 * scoring against the summary text.</p>
 */
public class SemanticRetriever {

    public SemanticRetriever() {}

    /**
     * Computes the cosine similarity between two float vectors of equal
     * length. Returns 0 if either vector has zero magnitude.
     *
     * @param a first vector
     * @param b second vector
     * @return cosine similarity in [-1, 1] (typically [0, 1] for embeddings)
     */
    public double cosineSimilarity(float[] a, float[] b) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) return 0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * Returns the {@code topK} summaries ranked by keyword relevance to
     * the query. This is a simple heuristic; when embedding support is
     * added, sort by cosine similarity instead.
     *
     * @param query     free-text search query
     * @param summaries the pool of summaries to search
     * @param topK      maximum number of results to return
     * @return summaries ordered from most to least relevant
     */
    public List<ConversationSummary> retrieveRelevant(
            String query, List<ConversationSummary> summaries, int topK) {
        return summaries.stream()
            .sorted((a, b) -> {
                int scoreA = relevanceScore(query, a);
                int scoreB = relevanceScore(query, b);
                return Integer.compare(scoreB, scoreA);
            })
            .limit(topK)
            .toList();
    }

    /**
     * Scores a summary by counting how many words of the query appear
     * in the summary text (case-insensitive).
     */
    private int relevanceScore(String query, ConversationSummary summary) {
        String lower = query.toLowerCase();
        int score = 0;
        for (String word : lower.split("\\s+")) {
            if (summary.summary().toLowerCase().contains(word)) score++;
        }
        return score;
    }
}
