package com.codingharness.memory;

import com.codingharness.llm.LlmResponse;
import com.codingharness.llm.MockLlmProvider;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class SummarySchedulerTest {

    @Test
    void shouldGenerateSummaryUsingMockLLM() {
        LlmResponse scriptedResponse = new LlmResponse(
                "The user discussed project architecture decisions.",
                List.of(),
                "stop",
                new LlmResponse.TokenUsage(50, 20, 70)
        );

        MockLlmProvider mockLlm = new MockLlmProvider(List.of(scriptedResponse));
        SummaryScheduler scheduler = new SummaryScheduler();

        List<SlidingWindowManager.TurnRecord> turns = List.of(
                SlidingWindowManager.TurnRecord.of("user", "What architecture should we use?"),
                SlidingWindowManager.TurnRecord.of("assistant", "Microservices would work well here."),
                SlidingWindowManager.TurnRecord.of("user", "Let's go with that.")
        );

        ConversationSummary summary = scheduler.generateSummary(turns, mockLlm);

        assertThat(summary).isNotNull();
        assertThat(summary.id()).isNotBlank();
        assertThat(summary.summary()).isEqualTo("The user discussed project architecture decisions.");
        assertThat(summary.createdAt()).isNotNull();
    }

    @Test
    void shouldExtractTopicsFromSummary() {
        LlmResponse scriptedResponse = new LlmResponse(
                "The user and assistant discussed Project Architecture and decided on Microservices.",
                List.of(),
                "stop",
                new LlmResponse.TokenUsage(50, 15, 65)
        );

        MockLlmProvider mockLlm = new MockLlmProvider(List.of(scriptedResponse));
        SummaryScheduler scheduler = new SummaryScheduler();

        List<SlidingWindowManager.TurnRecord> turns = List.of(
                SlidingWindowManager.TurnRecord.of("user", "hello")
        );

        ConversationSummary summary = scheduler.generateSummary(turns, mockLlm);

        assertThat(summary.topics()).isNotEmpty();
    }
}
