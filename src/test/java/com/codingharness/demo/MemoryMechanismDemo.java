package com.codingharness.demo;

import com.codingharness.llm.*;
import com.codingharness.memory.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mechanism Demo 3: Memory mechanism — the MAIN CONTRIBUTION dimension.
 * Demonstrates: sliding window, summary generation, semantic retrieval.
 * All deterministic with MockLlmProvider.
 */
class MemoryMechanismDemo {
    @Test
    void demo3_SlidingWindowManagement() {
        SlidingWindowManager swm = new SlidingWindowManager(20, 10);

        // Add 30 conversation turns
        for (int i = 0; i < 30; i++) {
            swm.addTurn(SlidingWindowManager.TurnRecord.of(
                i % 2 == 0 ? "user" : "assistant",
                "Message number " + i
            ));
        }

        // Window should return last 20 turns
        assertThat(swm.getWindowTurns()).hasSize(20);
        // Full history has all 30
        assertThat(swm.getFullHistory()).hasSize(30);
        // 30 % 10 == 0, should trigger summarization
        assertThat(swm.needsSummarization()).isTrue();
        // 30 - 20 = 10 turns need summarization
        assertThat(swm.getTurnsForSummarization()).hasSize(10);

        System.out.println("[DEMO 3.1] SlidingWindowManager: window=" + swm.getWindowTurns().size()
            + ", full=" + swm.getFullHistory().size()
            + ", needsSummary=" + swm.needsSummarization()
            + ", toSummarize=" + swm.getTurnsForSummarization().size());
    }

    @Test
    void demo3_SummaryGeneration() {
        // Script LLM to return a summary with actual content
        LlmResponse summaryResponse = new LlmResponse(
            "User showed interest in mystery novels and discussed Agatha Christie as a favorite author.",
            List.of(), "stop",
            new LlmResponse.TokenUsage(5, 3, 8));
        MockLlmProvider mockLlm = new MockLlmProvider(List.of(summaryResponse));

        SummaryScheduler scheduler = new SummaryScheduler();
        List<SlidingWindowManager.TurnRecord> turns = List.of(
            SlidingWindowManager.TurnRecord.of("user", "I love mystery novels"),
            SlidingWindowManager.TurnRecord.of("assistant", "Mystery novels are great! Who's your favorite author?"),
            SlidingWindowManager.TurnRecord.of("user", "Agatha Christie"),
            SlidingWindowManager.TurnRecord.of("assistant", "A classic choice!")
        );

        ConversationSummary summary = scheduler.generateSummary(turns, mockLlm);
        assertThat(summary).isNotNull();
        assertThat(summary.summary()).isNotEmpty();

        System.out.println("[DEMO 3.2] Summary generated: " + summary.summary());
    }

    @Test
    void demo3_SemanticRetrieval() {
        SemanticRetriever retriever = new SemanticRetriever();

        // Cosine similarity test
        float[] vecA = {1.0f, 0.0f, 0.0f};
        float[] vecB = {1.0f, 0.0f, 0.0f};
        double sim = retriever.cosineSimilarity(vecA, vecB);
        assertThat(sim).isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.001));
        System.out.println("[DEMO 3.3a] Cosine similarity of identical vectors: " + sim);

        float[] vecC = {1.0f, 0.0f, 0.0f};
        float[] vecD = {0.0f, 1.0f, 0.0f};
        double sim2 = retriever.cosineSimilarity(vecC, vecD);
        assertThat(sim2).isCloseTo(0.0, org.assertj.core.data.Offset.offset(0.001));
        System.out.println("[DEMO 3.3b] Cosine similarity of orthogonal vectors: " + sim2);

        // Keyword retrieval ranking
        List<ConversationSummary> summaries = List.of(
            new ConversationSummary("1", "User talked about favorite books and mystery novels", List.of("books"), java.time.Instant.now()),
            new ConversationSummary("2", "User discussed weather and weekend plans", List.of("weather"), java.time.Instant.now()),
            new ConversationSummary("3", "User shared thoughts on mystery authors like Christie", List.of("authors"), java.time.Instant.now())
        );

        List<ConversationSummary> results = retriever.retrieveRelevant("mystery books", summaries, 2);
        assertThat(results).hasSize(2);
        assertThat(results.get(0).id()).isEqualTo("1"); // Most relevant: mentions both mystery AND books
        System.out.println("[DEMO 3.3c] Top-2 results for 'mystery books': "
            + results.stream().map(ConversationSummary::id).toList());
    }

    @Test
    void demo3_FullMemoryPipeline() {
        // Full pipeline: add turns → auto-summarize → context includes summary
        InMemoryStore store = new InMemoryStore();
        LlmResponse summaryResp = new LlmResponse(
            "User and assistant discussed test messages in a conversation about coding.",
            List.of(), "stop",
            new LlmResponse.TokenUsage(2, 1, 3));
        MockLlmProvider mockLlm = new MockLlmProvider(List.of(summaryResp));

        ProjectMemoryRuntime runtime = new ProjectMemoryRuntime(20, 10, store, mockLlm);

        // Add 30 turns to trigger summarization at turn 10, 20, 30
        for (int i = 0; i < 30; i++) {
            runtime.addTurn(i % 2 == 0 ? "user" : "assistant", "Turn " + i + " message");
        }

        String context = runtime.getContextForLLM();
        assertThat(context).contains("## Recent Conversation");
        System.out.println("[DEMO 3.4] Full memory pipeline context length: " + context.length() + " chars");
        System.out.println("[DEMO 3.4] Window turns: " + runtime.getWindow().getWindowTurns().size());
    }
}
