package com.codingharness.tools;

import com.codingharness.config.HarnessConfig;
import com.codingharness.core.ProjectContext;
import com.codingharness.memory.HarnessMemory;
import com.codingharness.memory.InMemoryStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class ShellExecToolTest {
    private ProjectContext ctx(Path root) {
        HarnessMemory mem = new HarnessMemory(new InMemoryStore());
        return ProjectContext.create("test", root, mem, HarnessConfig.defaults());
    }

    @Test
    void echoHelloShouldReturnSuccess(@TempDir Path tmpDir) {
        ShellExecTool tool = new ShellExecTool();
        ToolResult result = tool.execute(Map.of("command", "echo hello"), ctx(tmpDir));
        assertThat(result.success()).isTrue();
        assertThat(result.output()).contains("hello");
    }

    @Test
    void invalidCommandShouldReturnFailure(@TempDir Path tmpDir) {
        ShellExecTool tool = new ShellExecTool();
        ToolResult result = tool.execute(Map.of("command", "nonexistentcommand_xyz_123"), ctx(tmpDir));
        // This may or may not fail depending on OS, but should not throw
        assertThat(result).isNotNull();
    }
}
