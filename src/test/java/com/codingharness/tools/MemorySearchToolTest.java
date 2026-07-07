package com.codingharness.tools;

import com.codingharness.config.HarnessConfig;
import com.codingharness.core.ProjectContext;
import com.codingharness.memory.HarnessMemory;
import com.codingharness.memory.InMemoryStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class MemorySearchToolTest {
    private HarnessMemory memory;
    private Path tmpDir;

    @BeforeEach
    void setUp(@TempDir Path tmpDir) {
        this.tmpDir = tmpDir;
        memory = new HarnessMemory(new InMemoryStore());
        memory.getStore().save("key1", "value one", Map.of("tag", "test"));
        memory.getStore().save("key2", "value two", Map.of());
        memory.getStore().save("key3", "another value", Map.of("category", "demo"));
    }

    private ProjectContext ctx() {
        return ProjectContext.create("test", tmpDir, memory, HarnessConfig.defaults());
    }

    @Test
    void searchShouldFindMatchingEntries() {
        MemorySearchTool tool = new MemorySearchTool();
        ToolResult result = tool.execute(Map.of("query", "value"), ctx());
        assertThat(result.success()).isTrue();
        assertThat(result.output()).contains("key1");
        assertThat(result.output()).contains("key2");
    }

    @Test
    void searchWithNoResultsShouldReturnMessage() {
        MemorySearchTool tool = new MemorySearchTool();
        ToolResult result = tool.execute(Map.of("query", "nonexistent"), ctx());
        assertThat(result.success()).isTrue();
        assertThat(result.output()).contains("No memory entries found");
    }
}
