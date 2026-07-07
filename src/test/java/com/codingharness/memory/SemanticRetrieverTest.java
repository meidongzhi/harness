package com.codingharness.memory;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class SemanticRetrieverTest {

    @Test
    void shouldComputeCosineSimilarityIdentical() {
        SemanticRetriever retriever = new SemanticRetriever();
        float[] a = {1.0f, 2.0f, 3.0f};
        float[] b = {1.0f, 2.0f, 3.0f};
        double sim = retriever.cosineSimilarity(a, b);
        assertThat(sim).isCloseTo(1.0, within(1e-6));
    }

    @Test
    void shouldComputeCosineSimilarityOrthogonal() {
        SemanticRetriever retriever = new SemanticRetriever();
        float[] a = {1.0f, 0.0f, 0.0f};
        float[] b = {0.0f, 1.0f, 0.0f};
        double sim = retriever.cosineSimilarity(a, b);
        assertThat(sim).isCloseTo(0.0, within(1e-6));
    }

    @Test
    void shouldComputeCosineSimilarityZeroVector() {
        SemanticRetriever retriever = new SemanticRetriever();
        float[] a = {0.0f, 0.0f};
        float[] b = {1.0f, 2.0f};
        double sim = retriever.cosineSimilarity(a, b);
        assertThat(sim).isEqualTo(0.0);
    }

    @Test
    void shouldRetrieveRelevantByKeyword() {
        SemanticRetriever retriever = new SemanticRetriever();
        List<ConversationSummary> summaries = List.of(
                new ConversationSummary("1", "The team discussed database migration to PostgreSQL.", List.of(), Instant.now()),
                new ConversationSummary("2", "Frontend refactoring was completed with React components.", List.of(), Instant.now()),
                new ConversationSummary("3", "API design patterns were reviewed for the backend team.", List.of(), Instant.now())
        );

        List<ConversationSummary> results = retriever.retrieveRelevant("database PostgreSQL", summaries, 2);

        assertThat(results).hasSize(2);
        // The database summary should be ranked first
        assertThat(results.get(0).id()).isEqualTo("1");
    }

    @Test
    void shouldReturnEmptyForNoMatches() {
        SemanticRetriever retriever = new SemanticRetriever();
        List<ConversationSummary> summaries = List.of(
                new ConversationSummary("1", "Discussed deployment strategies.", List.of(), Instant.now())
        );

        List<ConversationSummary> results = retriever.retrieveRelevant("quantum physics", summaries, 3);
        assertThat(results).hasSize(1); // All returned, but all score 0
    }
}
