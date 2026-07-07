package com.codingharness.tools;

import com.codingharness.config.HarnessConfig;
import com.codingharness.core.ProjectContext;
import com.codingharness.memory.HarnessMemory;
import com.codingharness.memory.InMemoryStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class ProjectSummaryToolTest {
    private ProjectContext ctx(Path root) {
        HarnessMemory mem = new HarnessMemory(new InMemoryStore());
        return ProjectContext.create("test-proj", root, mem, HarnessConfig.defaults());
    }

    @Test
    void summarizeEmptyProject(@TempDir Path tmpDir) {
        ProjectSummaryTool tool = new ProjectSummaryTool();
        ToolResult result = tool.execute(Map.of(), ctx(tmpDir));
        assertThat(result.success()).isTrue();
        assertThat(result.output()).contains("test-proj");
    }

    @Test
    void summarizeProjectWithPom(@TempDir Path tmpDir) throws Exception {
        String pom = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n"
            + "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
            + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"
            + "    <modelVersion>4.0.0</modelVersion>\n"
            + "    <groupId>com.example</groupId>\n"
            + "    <artifactId>my-lib</artifactId>\n"
            + "    <version>1.0.0</version>\n"
            + "</project>\n";
        Files.writeString(tmpDir.resolve("pom.xml"), pom);

        // Create some source files
        Path mainDir = tmpDir.resolve("src/main/java/com/example");
        Files.createDirectories(mainDir);
        Files.writeString(mainDir.resolve("App.java"), "package com.example;");
        Files.writeString(mainDir.resolve("Util.java"), "package com.example;");

        ProjectSummaryTool tool = new ProjectSummaryTool();
        ToolResult result = tool.execute(Map.of(), ctx(tmpDir));
        assertThat(result.success()).isTrue();
        assertThat(result.output()).contains("Maven");
        assertThat(result.output()).contains("Java files: 2");
    }
}
