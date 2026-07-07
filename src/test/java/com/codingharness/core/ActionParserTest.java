package com.codingharness.core;

import com.codingharness.llm.LlmResponse;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class ActionParserTest {
    @Test
    void shouldParseToolCallsIntoActions() {
        ActionParser parser = new ActionParser();
        LlmResponse response = new LlmResponse("I'll write a file", List.of(
            new LlmResponse.ToolCall("id1", "file_write",
                Map.of("path", "App.java", "content", "public class App {}")),
            new LlmResponse.ToolCall("id2", "test_run", Map.of())
        ), "tool_calls", new LlmResponse.TokenUsage(100, 50, 150));

        List<Action> actions = parser.parse(response);
        assertThat(actions).hasSize(2);
        assertThat(actions.get(0).type()).isEqualTo("file_write");
        assertThat(actions.get(0).parameters()).containsEntry("path", "App.java");
        assertThat(actions.get(1).type()).isEqualTo("test_run");
    }

    @Test
    void shouldReturnEmptyForNoToolCalls() {
        ActionParser parser = new ActionParser();
        LlmResponse response = new LlmResponse("Just some text", null,
            "stop", new LlmResponse.TokenUsage(10, 5, 15));

        List<Action> actions = parser.parse(response);
        assertThat(actions).isEmpty();
    }

    @Test
    void shouldReturnEmptyForEmptyToolCalls() {
        ActionParser parser = new ActionParser();
        LlmResponse response = new LlmResponse("Done", List.of(),
            "stop", new LlmResponse.TokenUsage(10, 5, 15));

        List<Action> actions = parser.parse(response);
        assertThat(actions).isEmpty();
    }
}
