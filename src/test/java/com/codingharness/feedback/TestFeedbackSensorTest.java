package com.codingharness.feedback;

import com.codingharness.config.HarnessConfig;
import com.codingharness.core.ProjectContext;
import com.codingharness.memory.HarnessMemory;
import com.codingharness.memory.InMemoryStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

class TestFeedbackSensorTest {

    private static final String MVN_PATH = detectMvn();

    private static String detectMvn() {
        // Try common locations
        String[] candidates = {
            "E:\\apache-maven-3.9.9\\bin\\mvn.cmd",
            "E:\\apache-maven-3.9.8\\bin\\mvn.cmd",
            "mvn"
        };
        for (String c : candidates) {
            try {
                new ProcessBuilder(c, "--version").start().waitFor();
                return c;
            } catch (Exception ignored) {}
        }
        return "mvn";
    }

    private ProjectContext ctx(Path root) {
        return ProjectContext.create("test", root,
            new HarnessMemory(new InMemoryStore()), HarnessConfig.defaults());
    }

    private void createMinimalMavenProject(Path root) throws Exception {
        String pomXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                     http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.test</groupId>
                <artifactId>test-project</artifactId>
                <version>1.0.0</version>
                <properties>
                    <maven.compiler.source>17</maven.compiler.source>
                    <maven.compiler.target>17</maven.compiler.target>
                    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
                </properties>
                <dependencies>
                    <dependency>
                        <groupId>org.junit.jupiter</groupId>
                        <artifactId>junit-jupiter</artifactId>
                        <version>5.10.2</version>
                        <scope>test</scope>
                    </dependency>
                </dependencies>
            </project>
            """;
        Files.writeString(root.resolve("pom.xml"), pomXml);

        // Create minimal main source so compilation doesn't fail
        Path mainJava = root.resolve("src/main/java/com/test");
        Files.createDirectories(mainJava);
        Files.writeString(mainJava.resolve("App.java"),
            "package com.test; public class App { public static void main(String[] args) {} }");

        // Create test directory
        Path testJava = root.resolve("src/test/java/com/test");
        Files.createDirectories(testJava);
    }

    @Test
    void shouldReturnAllPassedForProjectWithNoTests(@TempDir Path tmpDir) throws Exception {
        createMinimalMavenProject(tmpDir);

        TestFeedbackSensor sensor = new TestFeedbackSensor(MVN_PATH);
        FeedbackResult result = sensor.sense(ctx(tmpDir));

        assertThat(result).isNotNull();
        // With no test classes, Maven reports Tests run: 0 and BUILD SUCCESS
        assertThat(result.allPassed()).isTrue();
        assertThat(result.failures()).isEmpty();
    }

    @Test
    void shouldReturnAllPassedForProjectWithPassingTest(@TempDir Path tmpDir) throws Exception {
        createMinimalMavenProject(tmpDir);

        // Add a passing test
        Path testJava = tmpDir.resolve("src/test/java/com/test");
        Files.writeString(testJava.resolve("SampleTest.java"),
            """
            package com.test;
            import org.junit.jupiter.api.Test;
            import static org.junit.jupiter.api.Assertions.assertTrue;
            class SampleTest {
                @Test
                void shouldPass() { assertTrue(true); }
            }
            """);

        TestFeedbackSensor sensor = new TestFeedbackSensor(MVN_PATH);
        FeedbackResult result = sensor.sense(ctx(tmpDir));

        assertThat(result).isNotNull();
        assertThat(result.allPassed()).isTrue();
        assertThat(result.failures()).isEmpty();
    }

    @Test
    void shouldDetectFailingTest(@TempDir Path tmpDir) throws Exception {
        createMinimalMavenProject(tmpDir);

        // Add a failing test
        Path testJava = tmpDir.resolve("src/test/java/com/test");
        Files.writeString(testJava.resolve("FailingTest.java"),
            """
            package com.test;
            import org.junit.jupiter.api.Test;
            import static org.junit.jupiter.api.Assertions.fail;
            class FailingTest {
                @Test
                void shouldFail() { fail("expected failure"); }
            }
            """);

        TestFeedbackSensor sensor = new TestFeedbackSensor(MVN_PATH);
        FeedbackResult result = sensor.sense(ctx(tmpDir));

        assertThat(result).isNotNull();
        assertThat(result.allPassed()).isFalse();
    }
}
