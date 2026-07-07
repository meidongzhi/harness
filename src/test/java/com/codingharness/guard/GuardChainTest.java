package com.codingharness.guard;

import com.codingharness.config.HarnessConfig;
import com.codingharness.core.Action;
import com.codingharness.core.ProjectContext;
import com.codingharness.memory.HarnessMemory;
import com.codingharness.memory.InMemoryStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class GuardChainTest {
    private ProjectContext ctx(Path root) {
        return ProjectContext.create("test", root,
            new HarnessMemory(new InMemoryStore()), HarnessConfig.defaults());
    }

    @Test
    void shouldAllowWhenAllGuardsPass(@TempDir Path tmpDir) {
        GuardChain chain = new GuardChain(List.of(new FileGuard(), new ShellGuard()));
        Action action = new Action("file_write", Map.of("path", "src/App.java", "content", "x"));
        GuardResult result = chain.check(action, ctx(tmpDir));
        assertThat(result.allowed()).isTrue();
    }

    @Test
    void shouldBlockWhenFirstGuardBlocks(@TempDir Path tmpDir) {
        Guard blocking = (action, ctx) -> GuardResult.block("test block");
        Guard allowing = (action, ctx) -> GuardResult.allow();
        GuardChain chain = new GuardChain(List.of(blocking, allowing));
        Action action = new Action("test", Map.of());
        GuardResult result = chain.check(action, ctx(tmpDir));
        assertThat(result.allowed()).isFalse();
        assertThat(result.reason()).isEqualTo("test block");
    }

    @Test
    void shouldBlockWhenSecondGuardBlocks(@TempDir Path tmpDir) {
        Guard allowing = (action, ctx) -> GuardResult.allow();
        Guard blocking = (action, ctx) -> GuardResult.block("second block");
        GuardChain chain = new GuardChain(List.of(allowing, blocking));
        Action action = new Action("test", Map.of());
        GuardResult result = chain.check(action, ctx(tmpDir));
        assertThat(result.allowed()).isFalse();
        assertThat(result.reason()).isEqualTo("second block");
    }

    @Test
    void shouldAllowWithEmptyGuardList(@TempDir Path tmpDir) {
        GuardChain chain = new GuardChain(List.of());
        Action action = new Action("dangerous_action", Map.of());
        GuardResult result = chain.check(action, ctx(tmpDir));
        assertThat(result.allowed()).isTrue();
    }

    @Test
    void shouldStopAtFirstBlockingGuard(@TempDir Path tmpDir) {
        Guard firstBlocking = (action, ctx) -> GuardResult.block("first");
        Guard secondBlocking = (action, ctx) -> GuardResult.block("second");
        GuardChain chain = new GuardChain(List.of(firstBlocking, secondBlocking));
        Action action = new Action("test", Map.of());
        GuardResult result = chain.check(action, ctx(tmpDir));
        assertThat(result.allowed()).isFalse();
        assertThat(result.reason()).isEqualTo("first");
    }

    @Test
    void fileGuardShouldBlockSensitiveInChain(@TempDir Path tmpDir) {
        GuardChain chain = new GuardChain(List.of(new FileGuard(), new ShellGuard()));
        Action action = new Action("file_write", Map.of("path", ".env", "content", "SECRET=x"));
        GuardResult result = chain.check(action, ctx(tmpDir));
        assertThat(result.allowed()).isFalse();
        assertThat(result.reason()).contains("Sensitive");
    }
}
