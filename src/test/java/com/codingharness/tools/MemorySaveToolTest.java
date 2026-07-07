package com.codingharness.tools;

import com.codingharness.config.HarnessConfig;
import com.codingharness.core.ProjectContext;
import com.codingharness.memory.HarnessMemory;
import com.codingharness.memory.InMemoryStore;
import com.codingharness.memory.MemoryEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;

class MemorySaveToolTest {
    private ProjectContext ctx(Path root) {
        HarnessMemory mem = new HarnessMemory(new InMemoryStore());
        return ProjectContext.create("test", root, mem, HarnessConfig.defaults());
    }

    @Test
    void saveShouldStoreEntry(@TempDir Path tmpDir) {
        MemorySaveTool tool = new MemorySaveTool();
        ProjectContext context = ctx(tmpDir);

        ToolResult result = tool.execute(
            Map.of("key", "test-key", "value", "test value content"),
            context);
        assertThat(result.success()).isTrue();
        assertThat(result.output()).contains("test-key");

        Optional<MemoryEntry> saved = context.harnessMemory().getStore().get("test-key");
        assertThat(saved).isPresent();
        assertThat(saved.get().value()).isEqualTo("test value content");
    }

    @Test
    void saveWithMetadata(@TempDir Path tmpDir) {
        MemorySaveTool tool = new MemorySaveTool();
        ProjectContext context = ctx(tmpDir);

        ToolResult result = tool.execute(
            Map.of("key", "meta-key", "value", "content",
                   "metadata", Map.of("priority", "high", "type", "note")),
            context);
        assertThat(result.success()).isTrue();

        Optional<MemoryEntry> saved = context.harnessMemory().getStore().get("meta-key");
        assertThat(saved).isPresent();
        assertThat(saved.get().metadata()).containsEntry("priority", "high");
        assertThat(saved.get().metadata()).containsEntry("type", "note");
    }
}
