package com.codingharness;

import com.codingharness.config.HarnessConfig;
import com.codingharness.core.*;
import com.codingharness.memory.*;
import com.codingharness.llm.*;
import com.codingharness.tools.*;
import com.codingharness.guard.*;
import com.codingharness.feedback.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class IntegrationTest {
    @Test
    void shouldRunFullLoopWithMockLLM(@TempDir Path tmpDir) {
        // Setup
        HarnessConfig config = HarnessConfig.defaults();
        HarnessMemory memory = new HarnessMemory(new InMemoryStore());
        ProjectContext ctx = ProjectContext.create("test-app", tmpDir, memory, config);

        // Scripted LLM: create a file, then check tests
        LlmResponse r1 = new LlmResponse("I'll create the app", List.of(
            new LlmResponse.ToolCall("id1", "file_write",
                Map.of("path", "App.java", "content", "public class App {}"))
        ), "tool_calls", new LlmResponse.TokenUsage(10, 5, 15));

        LlmResponse r2 = new LlmResponse("Now let me run tests", List.of(
            new LlmResponse.ToolCall("id2", "test_run", Map.of())
        ), "tool_calls", new LlmResponse.TokenUsage(10, 5, 15));

        MockLlmProvider mockLlm = new MockLlmProvider(List.of(r1, r2));

        // Wire everything
        ToolRegistry tools = new ToolRegistry();
        tools.register(new FileWriteTool());
        tools.register(new TestRunTool());

        GuardChain guardChain = new GuardChain(List.of(new FileGuard(), new ShellGuard()));
        // Use a stub feedback sensor that returns success to test wiring end-to-end
        FeedbackSensor feedbackSensor = c -> FeedbackResult.allGood();

        AgentLoopImpl loop = new AgentLoopImpl(mockLlm, tools, guardChain, feedbackSensor, 30, 3);

        // Run
        var result = loop.run(ctx);
        assertThat(result).isIn(AgentLoopImpl.LoopResult.SUCCESS, AgentLoopImpl.LoopResult.MAX_TURNS);
    }
}
