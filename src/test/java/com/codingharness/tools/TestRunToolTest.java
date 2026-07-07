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

class TestRunToolTest {
    private ProjectContext ctx(Path root) {
        HarnessMemory mem = new HarnessMemory(new InMemoryStore());
        return ProjectContext.create("test", root, mem, HarnessConfig.defaults());
    }

    @Test
    void testRunShouldExecuteAndReturnResult(@TempDir Path tmpDir) {
        // This test verifies the tool runs and returns a result on a non-Maven directory
        TestRunTool tool = new TestRunTool();
        ToolResult result = tool.execute(Map.of(), ctx(tmpDir));
        // On a non-Maven project, it will fail but should not throw
        assertThat(result).isNotNull();
        // Should have either success or failure with output
        assertThat(result.output() + result.error()).isNotEmpty();
    }
}
