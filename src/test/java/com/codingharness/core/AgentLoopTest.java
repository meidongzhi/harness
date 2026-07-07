package com.codingharness.core;

import com.codingharness.config.HarnessConfig;
import com.codingharness.feedback.FeedbackResult;
import com.codingharness.feedback.FeedbackSensor;
import com.codingharness.guard.GuardChain;
import com.codingharness.guard.FileGuard;
import com.codingharness.guard.ShellGuard;
import com.codingharness.llm.LlmResponse;
import com.codingharness.llm.MockLlmProvider;
import com.codingharness.memory.HarnessMemory;
import com.codingharness.memory.InMemoryStore;
import com.codingharness.tools.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class AgentLoopTest {

    @Test
    void shouldExecuteSingleToolCallAndSucceed(@TempDir Path tmpDir) {
        // Scripted LLM: write a file, then test_run
        LlmResponse r1 = new LlmResponse("I'll create the app", List.of(
            new LlmResponse.ToolCall("id1", "file_write",
                Map.of("path", "App.java", "content", "public class App {}"))
        ), "tool_calls", new LlmResponse.TokenUsage(10, 5, 15));

        LlmResponse r2 = new LlmResponse("Now let me run tests", List.of(
            new LlmResponse.ToolCall("id2", "test_run", Map.of())
        ), "tool_calls", new LlmResponse.TokenUsage(10, 5, 15));

        MockLlmProvider mockLlm = new MockLlmProvider(List.of(r1, r2));

        // Wire up
        ToolRegistry tools = new ToolRegistry();
        tools.register(new FileWriteTool());
        tools.register(new TestRunTool());

        GuardChain guardChain = new GuardChain(List.of(new FileGuard(), new ShellGuard()));

        // Feedback sensor that always returns success so loop can terminate
        FeedbackSensor allPassSensor = ctx -> FeedbackResult.allGood();

        ProjectContext ctx = ProjectContext.create("test-app", tmpDir,
            new HarnessMemory(new InMemoryStore()), HarnessConfig.defaults());

        AgentLoopImpl loop = new AgentLoopImpl(mockLlm, tools, guardChain, allPassSensor, 10, 3);
        var result = loop.run(ctx);

        assertThat(result).isIn(AgentLoopImpl.LoopResult.SUCCESS, AgentLoopImpl.LoopResult.MAX_TURNS);

        // Verify file was actually written
        assertThat(tmpDir.resolve("App.java")).exists();
    }

    @Test
    void shouldReturnNeedsHumanAfterRepeatedFailures(@TempDir Path tmpDir) {
        // LLM keeps trying to run tests but tests always fail
        LlmResponse failingResponse = new LlmResponse("Trying again", List.of(
            new LlmResponse.ToolCall("id1", "test_run", Map.of())
        ), "tool_calls", new LlmResponse.TokenUsage(10, 5, 15));

        MockLlmProvider mockLlm = new MockLlmProvider(List.of(failingResponse));

        ToolRegistry tools = new ToolRegistry();
        tools.register(new TestRunTool());

        GuardChain guardChain = new GuardChain(List.of(new FileGuard(), new ShellGuard()));

        // Feedback sensor always returns failure
        FeedbackSensor failSensor = ctx -> new FeedbackResult(false,
            List.of(new FeedbackResult.TestFailure("Test.java", "testX", "assertion failed")),
            List.of(), List.of());

        ProjectContext ctx = ProjectContext.create("failing-app", tmpDir,
            new HarnessMemory(new InMemoryStore()), HarnessConfig.defaults());

        // maxRetries=2 so after 2 consecutive failures we get NEEDS_HUMAN
        AgentLoopImpl loop = new AgentLoopImpl(mockLlm, tools, guardChain, failSensor, 30, 2);
        var result = loop.run(ctx);

        assertThat(result).isEqualTo(AgentLoopImpl.LoopResult.NEEDS_HUMAN);
    }

    @Test
    void shouldHandleContentOnlyResponse(@TempDir Path tmpDir) {
        // LLM returns content-only (no tool calls) - should continue
        LlmResponse r1 = new LlmResponse("Thinking about the design approach...", null,
            "stop", new LlmResponse.TokenUsage(10, 5, 15));

        // Then it writes a file
        LlmResponse r2 = new LlmResponse("OK, writing the code now", List.of(
            new LlmResponse.ToolCall("id1", "file_write",
                Map.of("path", "Hello.java", "content", "public class Hello {}"))
        ), "tool_calls", new LlmResponse.TokenUsage(10, 5, 15));

        MockLlmProvider mockLlm = new MockLlmProvider(List.of(r1, r2));

        ToolRegistry tools = new ToolRegistry();
        tools.register(new FileWriteTool());
        tools.register(new TestRunTool());

        GuardChain guardChain = new GuardChain(List.of(new FileGuard(), new ShellGuard()));

        // Returns success so loop can finish
        FeedbackSensor allPassSensor = ctx -> FeedbackResult.allGood();

        ProjectContext ctx = ProjectContext.create("content-test", tmpDir,
            new HarnessMemory(new InMemoryStore()), HarnessConfig.defaults());

        AgentLoopImpl loop = new AgentLoopImpl(mockLlm, tools, guardChain, allPassSensor, 10, 3);
        var result = loop.run(ctx);

        // Should eventually succeed or hit max turns
        assertThat(result).isIn(AgentLoopImpl.LoopResult.SUCCESS, AgentLoopImpl.LoopResult.MAX_TURNS);
        assertThat(tmpDir.resolve("Hello.java")).exists();
    }
}
