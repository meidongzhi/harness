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

class FileToolsTest {
    private ProjectContext ctx(Path root) {
        HarnessMemory mem = new HarnessMemory(new InMemoryStore());
        return ProjectContext.create("test", root, mem, HarnessConfig.defaults());
    }

    @Test
    void fileReadShouldReadExistingFile(@TempDir Path tmpDir) throws Exception {
        Path file = tmpDir.resolve("test.txt");
        java.nio.file.Files.writeString(file, "hello world");
        FileReadTool tool = new FileReadTool();
        ToolResult result = tool.execute(Map.of("path", "test.txt"), ctx(tmpDir));
        assertThat(result.success()).isTrue();
        assertThat(result.output()).contains("hello world");
    }

    @Test
    void fileWriteShouldCreateFile(@TempDir Path tmpDir) {
        FileWriteTool tool = new FileWriteTool();
        ToolResult result = tool.execute(Map.of("path", "output.txt", "content", "test content"), ctx(tmpDir));
        assertThat(result.success()).isTrue();
        assertThat(tmpDir.resolve("output.txt")).exists();
    }

    @Test
    void fileDeleteShouldRemoveFile(@TempDir Path tmpDir) throws Exception {
        Path file = tmpDir.resolve("todelete.txt");
        java.nio.file.Files.writeString(file, "delete me");
        FileDeleteTool tool = new FileDeleteTool();
        ToolResult result = tool.execute(Map.of("path", "todelete.txt"), ctx(tmpDir));
        assertThat(result.success()).isTrue();
        assertThat(file).doesNotExist();
    }
}
