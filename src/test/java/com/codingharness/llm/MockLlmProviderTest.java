package com.codingharness.llm;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class MockLlmProviderTest {
    @Test
    void shouldReturnScriptedResponsesInOrder() {
        LlmResponse r1 = new LlmResponse("hello", List.of(), "stop",
            new LlmResponse.TokenUsage(10, 5, 15));
        LlmResponse r2 = new LlmResponse("world", List.of(), "stop",
            new LlmResponse.TokenUsage(8, 4, 12));
        MockLlmProvider mock = new MockLlmProvider(List.of(r1, r2));
        LlmRequest req = new LlmRequest("test", List.of(), List.of(), 100, 0.0);
        assertThat(mock.complete(req).content()).isEqualTo("hello");
        assertThat(mock.complete(req).content()).isEqualTo("world");
    }

    @Test
    void shouldReturnLastResponseWhenQueueExhausted() {
        LlmResponse r = new LlmResponse("final", List.of(), "stop",
            new LlmResponse.TokenUsage(1, 1, 2));
        MockLlmProvider mock = new MockLlmProvider(List.of(r));
        LlmRequest req = new LlmRequest("test", List.of(), List.of(), 100, 0.0);
        assertThat(mock.complete(req).content()).isEqualTo("final");
        assertThat(mock.complete(req).content()).isEqualTo("final");
    }

    @Test
    void shouldThrowWhenInitializedWithEmptyList() {
        assertThatThrownBy(() -> new MockLlmProvider(List.of()))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
