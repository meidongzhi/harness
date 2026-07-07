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

class ScaffoldNewToolTest {
    private ProjectContext ctx(Path root) {
        HarnessMemory mem = new HarnessMemory(new InMemoryStore());
        return ProjectContext.create("test", root, mem, HarnessConfig.defaults());
    }

    @Test
    void scaffoldJavaMavenProject(@TempDir Path tmpDir) {
        ScaffoldNewTool tool = new ScaffoldNewTool();
        ToolResult result = tool.execute(
            Map.of("name", "my-app", "type", "java-maven"), ctx(tmpDir));
        assertThat(result.success()).isTrue();
        assertThat(result.output()).contains("java-maven");
        assertThat(tmpDir.resolve("my-app/pom.xml")).exists();
        assertThat(tmpDir.resolve("my-app/src/main/java")).isDirectory();
    }

    @Test
    void scaffoldNodeProject(@TempDir Path tmpDir) {
        ScaffoldNewTool tool = new ScaffoldNewTool();
        ToolResult result = tool.execute(
            Map.of("name", "my-node-app", "type", "node"), ctx(tmpDir));
        assertThat(result.success()).isTrue();
        assertThat(tmpDir.resolve("my-node-app/package.json")).exists();
        assertThat(tmpDir.resolve("my-node-app/src/index.js")).exists();
    }

    @Test
    void scaffoldPythonProject(@TempDir Path tmpDir) {
        ScaffoldNewTool tool = new ScaffoldNewTool();
        ToolResult result = tool.execute(
            Map.of("name", "my-python-app", "type", "python"), ctx(tmpDir));
        assertThat(result.success()).isTrue();
        assertThat(tmpDir.resolve("my-python-app/requirements.txt")).exists();
    }

    @Test
    void unsupportedTypeShouldFail(@TempDir Path tmpDir) {
        ScaffoldNewTool tool = new ScaffoldNewTool();
        ToolResult result = tool.execute(
            Map.of("name", "test", "type", "unsupported"), ctx(tmpDir));
        assertThat(result.success()).isFalse();
        assertThat(result.error()).contains("unsupported");
    }
}
