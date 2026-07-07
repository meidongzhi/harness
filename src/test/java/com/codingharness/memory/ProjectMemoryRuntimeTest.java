package com.codingharness.memory;

import com.codingharness.llm.LlmResponse;
import com.codingharness.llm.MockLlmProvider;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class ProjectMemoryRuntimeTest {

    @Test
    void shouldTrackTurnsInWindow() {
        ProjectMemoryRuntime rt = new ProjectMemoryRuntime(20, 10,
                new InMemoryStore(),
                new MockLlmProvider(List.of(dummyResponse())));

        rt.addTurn("user", "Hello");
        rt.addTurn("assistant", "Hi there!");

        assertThat(rt.getWindow().totalTurns()).isEqualTo(2);
        assertThat(rt.getWindow().getWindowTurns()).hasSize(2);
    }

    @Test
    void shouldGenerateSummaryWhenThresholdReached() {
        // threshold of 5, window size 3
        // After 5 turns, summarization should trigger
        // overflow = 5 - 3 = 2 turns to summarize
        MockLlmProvider mockLlm = new MockLlmProvider(List.of(dummyResponse()));
        InMemoryStore summaryStore = new InMemoryStore();

        ProjectMemoryRuntime rt = new ProjectMemoryRuntime(3, 5,
                summaryStore, mockLlm);

        for (int i = 0; i < 5; i++) {
            rt.addTurn("user", "message " + i);
        }

        // After 5 turns, summarization should have stored a summary
        List<MemoryEntry> summaries = summaryStore.listRecent(10);
        assertThat(summaries).hasSize(1);
        assertThat(summaries.get(0).value()).isEqualTo("Mock summary response.");
    }

    @Test
    void shouldBuildContextForLLM() {
        MockLlmProvider mockLlm = new MockLlmProvider(List.of(dummyResponse()));
        InMemoryStore summaryStore = new InMemoryStore();
        // pre-populate a summary
        summaryStore.save("summary:abc", "Previous discussion about architecture.",
                java.util.Map.of("topics", "Architecture"));

        ProjectMemoryRuntime rt = new ProjectMemoryRuntime(20, 10,
                summaryStore, mockLlm);

        rt.addTurn("user", "What should we do next?");
        rt.addTurn("assistant", "Let's review the architecture.");

        String context = rt.getContextForLLM();

        assertThat(context).contains("## Recent Conversation");
        assertThat(context).contains("user: What should we do next?");
        assertThat(context).contains("assistant: Let's review the architecture.");
        assertThat(context).contains("## Relevant History");
        assertThat(context).contains("Previous discussion about architecture.");
    }

    @Test
    void shouldNotSummarizeBeforeThreshold() {
        MockLlmProvider mockLlm = new MockLlmProvider(List.of(dummyResponse()));
        InMemoryStore summaryStore = new InMemoryStore();

        ProjectMemoryRuntime rt = new ProjectMemoryRuntime(20, 10,
                summaryStore, mockLlm);

        for (int i = 0; i < 5; i++) {
            rt.addTurn("user", "message " + i);
        }

        // No summary should be generated before threshold
        List<MemoryEntry> summaries = summaryStore.listRecent(10);
        assertThat(summaries).isEmpty();
    }

    private static LlmResponse dummyResponse() {
        return new LlmResponse(
                "Mock summary response.",
                List.of(),
                "stop",
                new LlmResponse.TokenUsage(10, 5, 15)
        );
    }
}
