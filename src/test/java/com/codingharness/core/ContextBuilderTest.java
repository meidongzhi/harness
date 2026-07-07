package com.codingharness.core;

import com.codingharness.config.HarnessConfig;
import com.codingharness.feedback.FeedbackResult;
import com.codingharness.memory.HarnessMemory;
import com.codingharness.memory.InMemoryStore;
import com.codingharness.tools.ToolRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class ContextBuilderTest {
    @Test
    void shouldIncludeSystemPrompt(@TempDir Path tmpDir) {
        ContextBuilder builder = new ContextBuilder();
        ProjectContext ctx = ProjectContext.create("test", tmpDir,
            new HarnessMemory(new InMemoryStore()), HarnessConfig.defaults());
        var request = builder.build(new ToolRegistry(), ctx, List.of(), null);
        assertThat(request.messages()).isNotEmpty();
        assertThat(request.messages().get(0).role()).isEqualTo("system");
    }

    @Test
    void shouldIncludeFeedbackWhenPresent(@TempDir Path tmpDir) {
        ContextBuilder builder = new ContextBuilder();
        ProjectContext ctx = ProjectContext.create("test", tmpDir,
            new HarnessMemory(new InMemoryStore()), HarnessConfig.defaults());
        FeedbackResult feedback = new FeedbackResult(false,
            List.of(new FeedbackResult.TestFailure("AppTest.java", "testApp", "expected 200 got 500")),
            List.of(), List.of());
        var request = builder.build(new ToolRegistry(), ctx, List.of(), feedback);
        String lastMsg = request.messages().get(request.messages().size() - 1).content();
        assertThat(lastMsg).contains("Test failure");
        assertThat(lastMsg).contains("expected 200 got 500");
    }
}
