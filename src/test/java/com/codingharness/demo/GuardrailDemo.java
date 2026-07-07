package com.codingharness.demo;

import com.codingharness.config.HarnessConfig;
import com.codingharness.core.Action;
import com.codingharness.core.ProjectContext;
import com.codingharness.guard.FileGuard;
import com.codingharness.guard.GuardResult;
import com.codingharness.guard.ShellGuard;
import com.codingharness.memory.HarnessMemory;
import com.codingharness.memory.InMemoryStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mechanism Demo 1: Guardrail interception of dangerous actions.
 * This test demonstrates DETERMINISTIC guardrail behavior — no LLM involved.
 */
class GuardrailDemo {
    private ProjectContext ctx(Path root) {
        return ProjectContext.create("demo", root,
            new HarnessMemory(new InMemoryStore()), HarnessConfig.defaults());
    }

    @Test
    void demo1_ShellGuardBlocksDangerousCommand(@TempDir Path tmpDir) {
        ShellGuard guard = new ShellGuard();

        // Demonstrate: rm -rf / is blocked
        Action dangerous = new Action("shell_exec", Map.of("command", "rm -rf /"));
        GuardResult result = guard.check(dangerous, ctx(tmpDir));
        assertThat(result.allowed()).isFalse();
        assertThat(result.reason()).contains("rm");
        System.out.println("[DEMO 1.1] ShellGuard blocked 'rm -rf /': " + result.reason());

        // Demonstrate: sudo is blocked
        Action sudoCmd = new Action("shell_exec", Map.of("command", "sudo rm file.txt"));
        GuardResult result2 = guard.check(sudoCmd, ctx(tmpDir));
        assertThat(result2.allowed()).isFalse();
        System.out.println("[DEMO 1.2] ShellGuard blocked 'sudo rm': " + result2.reason());

        // Demonstrate: safe command passes
        Action safe = new Action("shell_exec", Map.of("command", "mvn test"));
        GuardResult result3 = guard.check(safe, ctx(tmpDir));
        assertThat(result3.allowed()).isTrue();
        System.out.println("[DEMO 1.3] ShellGuard allowed 'mvn test'");
    }

    @Test
    void demo1_FileGuardBlocksDangerousPaths(@TempDir Path tmpDir) {
        FileGuard guard = new FileGuard();

        // Demonstrate: writing outside project is blocked
        Action outside = new Action("file_write", Map.of("path", "/etc/passwd", "content", "hacked"));
        GuardResult result = guard.check(outside, ctx(tmpDir));
        assertThat(result.allowed()).isFalse();
        System.out.println("[DEMO 1.4] FileGuard blocked write to /etc/passwd: " + result.reason());

        // Demonstrate: writing .env is blocked
        Action env = new Action("file_write", Map.of("path", ".env", "content", "SECRET=xxx"));
        GuardResult result2 = guard.check(env, ctx(tmpDir));
        assertThat(result2.allowed()).isFalse();
        System.out.println("[DEMO 1.5] FileGuard blocked .env file: " + result2.reason());

        // Demonstrate: normal file write passes
        Action normal = new Action("file_write", Map.of("path", "src/App.java", "content", "class App {}"));
        GuardResult result3 = guard.check(normal, ctx(tmpDir));
        assertThat(result3.allowed()).isTrue();
        System.out.println("[DEMO 1.6] FileGuard allowed normal file write");
    }
}
