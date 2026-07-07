package com.codingharness.guard;

import com.codingharness.config.HarnessConfig;
import com.codingharness.core.Action;
import com.codingharness.core.ProjectContext;
import com.codingharness.memory.HarnessMemory;
import com.codingharness.memory.InMemoryStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class GuardTest {
    private ProjectContext ctx(Path root) {
        return ProjectContext.create("test", root,
            new HarnessMemory(new InMemoryStore()), HarnessConfig.defaults());
    }

    // FileGuard tests
    @Test
    void fileGuardShouldAllowWriteInsideProject(@TempDir Path tmpDir) {
        FileGuard guard = new FileGuard();
        Action action = new Action("file_write", Map.of("path", "src/main/App.java", "content", "x"));
        GuardResult result = guard.check(action, ctx(tmpDir));
        assertThat(result.allowed()).isTrue();
    }

    @Test
    void fileGuardShouldBlockWriteOutsideProject(@TempDir Path tmpDir) {
        FileGuard guard = new FileGuard();
        Action action = new Action("file_write", Map.of("path", "/etc/passwd", "content", "x"));
        GuardResult result = guard.check(action, ctx(tmpDir));
        assertThat(result.allowed()).isFalse();
    }

    @Test
    void fileGuardShouldBlockEnvFile(@TempDir Path tmpDir) {
        FileGuard guard = new FileGuard();
        Action action = new Action("file_write", Map.of("path", ".env", "content", "SECRET=x"));
        GuardResult result = guard.check(action, ctx(tmpDir));
        assertThat(result.allowed()).isFalse();
    }

    // ShellGuard tests
    @Test
    void shellGuardShouldAllowMvnCommand(@TempDir Path tmpDir) {
        ShellGuard guard = new ShellGuard();
        Action action = new Action("shell_exec", Map.of("command", "mvn test"));
        GuardResult result = guard.check(action, ctx(tmpDir));
        assertThat(result.allowed()).isTrue();
    }

    @Test
    void shellGuardShouldBlockRmRf(@TempDir Path tmpDir) {
        ShellGuard guard = new ShellGuard();
        Action action = new Action("shell_exec", Map.of("command", "rm -rf /"));
        GuardResult result = guard.check(action, ctx(tmpDir));
        assertThat(result.allowed()).isFalse();
    }

    @Test
    void shellGuardShouldBlockSudo(@TempDir Path tmpDir) {
        ShellGuard guard = new ShellGuard();
        Action action = new Action("shell_exec", Map.of("command", "sudo rm file.txt"));
        GuardResult result = guard.check(action, ctx(tmpDir));
        assertThat(result.allowed()).isFalse();
    }
}
