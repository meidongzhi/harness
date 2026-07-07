package com.codingharness.scaffold;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.*;

class ScaffolderTest {

    @Test
    void shouldGenerateCompleteProjectStructure(@TempDir Path tmpDir) throws Exception {
        Scaffolder scaffolder = new Scaffolder();
        Path projectRoot = scaffolder.scaffold("My Test App", "A test application", tmpDir);

        // Verify directory exists
        assertThat(projectRoot).exists();
        assertThat(projectRoot).isDirectory();

        // Verify key files exist
        assertThat(projectRoot.resolve("pom.xml")).exists();
        assertThat(projectRoot.resolve("src/main/java/com/example/mytestapp/Application.java")).exists();
        assertThat(projectRoot.resolve("src/main/java/com/example/mytestapp/ChatController.java")).exists();
        assertThat(projectRoot.resolve("src/main/java/com/example/mytestapp/MemoryConfig.java")).exists();
        assertThat(projectRoot.resolve("src/main/resources/templates/index.html")).exists();
        assertThat(projectRoot.resolve("src/main/resources/application.yml")).exists();
        assertThat(projectRoot.resolve("src/test/java/com/example/mytestapp/ApplicationTest.java")).exists();
        assertThat(projectRoot.resolve(".gitignore")).exists();
    }

    @Test
    void shouldGenerateValidPomXml(@TempDir Path tmpDir) throws Exception {
        Scaffolder scaffolder = new Scaffolder();
        Path projectRoot = scaffolder.scaffold("TestPom", "Pom test", tmpDir);

        String pomContent = Files.readString(projectRoot.resolve("pom.xml"));
        assertThat(pomContent).contains("spring-boot-starter-parent");
        assertThat(pomContent).contains("spring-boot-starter-web");
        assertThat(pomContent).contains("spring-boot-starter-thymeleaf");
        assertThat(pomContent).contains("spring-boot-maven-plugin");
        assertThat(pomContent).contains("<groupId>com.example</groupId>");
        assertThat(pomContent).contains("<artifactId>testpom</artifactId>");
        assertThat(pomContent).contains("<name>TestPom</name>");
    }

    @Test
    void shouldGenerateValidController(@TempDir Path tmpDir) throws Exception {
        Scaffolder scaffolder = new Scaffolder();
        Path projectRoot = scaffolder.scaffold("ChatApp", "A chat app", tmpDir);

        String controllerContent = Files.readString(
            projectRoot.resolve("src/main/java/com/example/chatapp/ChatController.java"));
        assertThat(controllerContent).contains("@RestController");
        assertThat(controllerContent).contains("@PostMapping(\"/chat\")");
        assertThat(controllerContent).contains("processMessage");
    }

    @Test
    void shouldGenerateIndexHtmlWithChatUI(@TempDir Path tmpDir) throws Exception {
        Scaffolder scaffolder = new Scaffolder();
        Path projectRoot = scaffolder.scaffold("WebApp", "A web app", tmpDir);

        String htmlContent = Files.readString(
            projectRoot.resolve("src/main/resources/templates/index.html"));
        assertThat(htmlContent).contains("<!DOCTYPE html>");
        assertThat(htmlContent).contains("sendMessage()");
        assertThat(htmlContent).contains("/chat");
        assertThat(htmlContent).contains("WebApp");
    }

    @Test
    void shouldGenerateMemoryConfig(@TempDir Path tmpDir) throws Exception {
        Scaffolder scaffolder = new Scaffolder();
        Path projectRoot = scaffolder.scaffold("MemApp", "Memory app", tmpDir);

        String configContent = Files.readString(
            projectRoot.resolve("src/main/java/com/example/memapp/MemoryConfig.java"));
        assertThat(configContent).contains("ProjectMemoryRuntime");
        assertThat(configContent).contains("InMemoryStore");
        assertThat(configContent).contains("@Configuration");
    }
}
