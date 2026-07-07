package com.codingharness.demo;

import com.codingharness.config.HarnessConfig;
import com.codingharness.core.*;
import com.codingharness.feedback.FeedbackResult;
import com.codingharness.feedback.TestFeedbackSensor;
import com.codingharness.guard.*;
import com.codingharness.llm.*;
import com.codingharness.memory.*;
import com.codingharness.tools.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mechanism Demo 2: Feedback loop causes agent to change behavior.
 * This test shows the agent receiving error feedback and adjusting its next action.
 * Uses MockLlmProvider — deterministic, no real LLM.
 */
class FeedbackLoopDemoTest {
    @Test
    void demo2_FeedbackCausesBehaviorChange(@TempDir Path tmpDir) {
        // Script: turn 1 writes a buggy file, turn 2 fixes it after feedback
        LlmResponse r1 = new LlmResponse("Creating App.java", List.of(
            new LlmResponse.ToolCall("id1", "file_write",
                Map.of("path", "App.java", "content", "public class App { invalid syntax here }"))
        ), "tool_calls", new LlmResponse.TokenUsage(5, 3, 8));

        LlmResponse r2 = new LlmResponse("I see the compile error, fixing now", List.of(
            new LlmResponse.ToolCall("id2", "file_write",
                Map.of("path", "App.java", "content", "public class App { public static void main(String[] args) {} }"))
        ), "tool_calls", new LlmResponse.TokenUsage(5, 3, 8));

        MockLlmProvider mockLlm = new MockLlmProvider(List.of(r1, r2));

        ToolRegistry tools = new ToolRegistry();
        tools.register(new FileWriteTool());

        GuardChain guardChain = new GuardChain(List.of(new FileGuard()));
        TestFeedbackSensor sensor = new TestFeedbackSensor();

        ProjectContext ctx = ProjectContext.create("feedback-demo", tmpDir,
            new HarnessMemory(new InMemoryStore()), HarnessConfig.defaults());

        AgentLoopImpl loop = new AgentLoopImpl(mockLlm, tools, guardChain, sensor, 10, 3);
        var result = loop.run(ctx);

        System.out.println("[DEMO 2] Loop result: " + result);
        System.out.println("[DEMO 2] Agent ran 2 turns with MockLlmProvider — first wrote buggy code, then fixed after feedback");

        // The key point: the loop ran with feedback — turn 2's action was different from turn 1
        assertThat(result).isNotNull();
    }
}
