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

class DependencyAddToolTest {
    private ProjectContext ctx(Path root) {
        HarnessMemory mem = new HarnessMemory(new InMemoryStore());
        return ProjectContext.create("test", root, mem, HarnessConfig.defaults());
    }

    @Test
    void addDependencyToPomXml(@TempDir Path tmpDir) throws Exception {
        String pom = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n"
            + "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
            + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"
            + "    <modelVersion>4.0.0</modelVersion>\n"
            + "    <groupId>com.test</groupId>\n"
            + "    <artifactId>test</artifactId>\n"
            + "    <version>1.0.0</version>\n"
            + "    <dependencies>\n"
            + "    </dependencies>\n"
            + "</project>\n";
        Files.writeString(tmpDir.resolve("pom.xml"), pom);

        DependencyAddTool tool = new DependencyAddTool();
        ToolResult result = tool.execute(
            Map.of("groupId", "com.google.guava", "artifactId", "guava", "version", "33.0-jre"),
            ctx(tmpDir));
        assertThat(result.success()).isTrue();
        assertThat(result.output()).contains("guava");

        String updated = Files.readString(tmpDir.resolve("pom.xml"));
        assertThat(updated).contains("com.google.guava");
        assertThat(updated).contains("guava");
        assertThat(updated).contains("33.0-jre");
    }

    @Test
    void noPomXmlShouldFail(@TempDir Path tmpDir) {
        DependencyAddTool tool = new DependencyAddTool();
        ToolResult result = tool.execute(
            Map.of("groupId", "com.test", "artifactId", "lib", "version", "1.0"),
            ctx(tmpDir));
        assertThat(result.success()).isFalse();
        assertThat(result.error()).contains("pom.xml not found");
    }
}
