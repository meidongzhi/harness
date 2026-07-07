package com.codingharness.core;

import com.codingharness.llm.LlmRequest;
import com.codingharness.llm.LlmRequest.Message;
import com.codingharness.config.HarnessConfig;
import com.codingharness.feedback.FeedbackResult;
import com.codingharness.tools.ToolRegistry;
import java.util.ArrayList;
import java.util.List;

public class ContextBuilder {
    private static final String SYSTEM_PROMPT = """
        You are a coding agent that builds AI companion web applications.
        You have access to tools for file operations, shell commands, testing, and project scaffolding.

        Your goal is to generate a complete, working web application based on the user's description.
        Follow these rules:
        1. Generate complete, compilable code
        2. Run tests after generating code
        3. If tests fail, analyze the failure and fix the code
        4. Do not delete files outside the project directory
        5. Do not hardcode API keys in generated code
        6. Each generated project must include the ProjectMemoryRuntime for conversation memory
        """;

    public LlmRequest build(ToolRegistry tools, ProjectContext ctx, List<String> history, FeedbackResult feedback) {
        List<Message> messages = new ArrayList<>();
        messages.add(Message.system(SYSTEM_PROMPT));
        messages.add(Message.user("Working directory: " + ctx.projectRoot()));
        messages.add(Message.user("Project: " + ctx.projectName()));

        // Add history
        for (String h : history) {
            messages.add(Message.user(h));
        }

        // Add feedback if present
        if (feedback != null) {
            StringBuilder fb = new StringBuilder("## Feedback from last action\n");
            if (!feedback.allPassed()) {
                for (var err : feedback.errors()) {
                    fb.append("Compile error in ").append(err.file()).append(":").append(err.line())
                      .append(" - ").append(err.message()).append("\n");
                }
                for (var fail : feedback.failures()) {
                    fb.append("Test failure in ").append(fail.file())
                      .append(" - ").append(fail.testName()).append(": ").append(fail.message()).append("\n");
                }
            } else {
                fb.append("All tests passed.\n");
            }
            messages.add(Message.user(fb.toString()));
        }

        HarnessConfig config = ctx.config();
        return new LlmRequest(
            config.model(),
            messages,
            tools.listForLLM(),
            Math.min(config.maxTurns() * 500, 4096),
            0.7
        );
    }
}
