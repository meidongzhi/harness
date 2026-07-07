package com.codingharness.scaffold;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Generates a complete Maven/Spring Boot project scaffold from templates.
 *
 * <p>All templates are embedded as Java String constants to avoid
 * classpath-loading issues at runtime.</p>
 */
public class Scaffolder {

    private static final String POM_TEMPLATE = """
        <?xml version="1.0" encoding="UTF-8"?>
        <project xmlns="http://maven.apache.org/POM/4.0.0"
                 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                 xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
            <modelVersion>4.0.0</modelVersion>
            <parent>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-parent</artifactId>
                <version>3.2.2</version>
                <relativePath/>
            </parent>
            <groupId>{{groupId}}</groupId>
            <artifactId>{{artifactId}}</artifactId>
            <version>0.0.1-SNAPSHOT</version>
            <name>{{projectName}}</name>
            <description>{{description}}</description>
            <properties>
                <java.version>17</java.version>
            </properties>
            <dependencies>
                <dependency>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-web</artifactId>
                </dependency>
                <dependency>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-thymeleaf</artifactId>
                </dependency>
                <dependency>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-test</artifactId>
                    <scope>test</scope>
                </dependency>
            </dependencies>
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-maven-plugin</artifactId>
                    </plugin>
                </plugins>
            </build>
        </project>
        """;

    private static final String APPLICATION_TEMPLATE = """
        package {{packageName}};

        import org.springframework.boot.SpringApplication;
        import org.springframework.boot.autoconfigure.SpringBootApplication;

        @SpringBootApplication
        public class Application {
            public static void main(String[] args) {
                SpringApplication.run(Application.class, args);
            }
        }
        """;

    private static final String CHAT_CONTROLLER_TEMPLATE = """
        package {{packageName}};

        import org.springframework.web.bind.annotation.*;
        import java.util.*;

        @RestController
        public class ChatController {

            private final List<Map<String, String>> history = new ArrayList<>();

            @PostMapping("/chat")
            public Map<String, Object> chat(@RequestBody Map<String, String> request) {
                String message = request.getOrDefault("message", "");
                String response = processMessage(message);
                return Map.of("response", response);
            }

            @GetMapping("/history")
            public List<Map<String, String>> getHistory() {
                return history;
            }

            private String processMessage(String message) {
                history.add(Map.of("user", message));
                String reply = "Echo: " + message;
                history.add(Map.of("assistant", reply));
                return reply;
            }
        }
        """;

    private static final String INDEX_HTML_TEMPLATE = """
        <!DOCTYPE html>
        <html xmlns:th="http://www.thymeleaf.org">
        <head>
            <meta charset="UTF-8">
            <title>{{projectName}}</title>
            <style>
                body { font-family: sans-serif; max-width: 600px; margin: 50px auto; padding: 20px; }
                #chat { border: 1px solid #ccc; border-radius: 8px; padding: 20px; }
                #messages { height: 300px; overflow-y: auto; border: 1px solid #eee; padding: 10px; margin-bottom: 10px; }
                .user { color: #0066cc; }
                .assistant { color: #006600; }
                input[type="text"] { width: 80%; padding: 8px; }
                button { padding: 8px 16px; }
            </style>
        </head>
        <body>
            <h1>{{projectName}}</h1>
            <div id="chat">
                <div id="messages"></div>
                <input type="text" id="input" placeholder="Type a message..." />
                <button onclick="sendMessage()">Send</button>
            </div>
            <script>
                async function sendMessage() {
                    const input = document.getElementById('input');
                    const msg = input.value.trim();
                    if (!msg) return;
                    input.value = '';
                    addMessage('user', msg);
                    try {
                        const res = await fetch('/chat', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify({ message: msg })
                        });
                        const data = await res.json();
                        addMessage('assistant', data.response);
                    } catch (e) {
                        addMessage('assistant', 'Error: ' + e.message);
                    }
                }
                function addMessage(role, text) {
                    const div = document.createElement('div');
                    div.className = role;
                    div.textContent = role + ': ' + text;
                    document.getElementById('messages').appendChild(div);
                }
                document.getElementById('input').addEventListener('keypress', (e) => {
                    if (e.key === 'Enter') sendMessage();
                });
            </script>
        </body>
        </html>
        """;

    private static final String APPLICATION_YML_TEMPLATE = """
        server:
          port: 8080

        spring:
          thymeleaf:
            prefix: classpath:/templates/
            suffix: .html

        logging:
          level:
            root: INFO
        """;

    private static final String MEMORY_CONFIG_TEMPLATE = """
        package {{packageName}};

        import com.codingharness.memory.ProjectMemoryRuntime;
        import com.codingharness.memory.InMemoryStore;
        import com.codingharness.llm.LlmProvider;
        import org.springframework.context.annotation.Bean;
        import org.springframework.context.annotation.Configuration;

        @Configuration
        public class MemoryConfig {

            @Bean
            public ProjectMemoryRuntime projectMemoryRuntime(LlmProvider llmProvider) {
                return new ProjectMemoryRuntime(20, 10, new InMemoryStore(), llmProvider);
            }
        }
        """;

    private static final String TEST_TEMPLATE = """
        package {{packageName}};

        import org.junit.jupiter.api.Test;
        import org.springframework.boot.test.context.SpringBootTest;
        import static org.assertj.core.api.Assertions.*;

        @SpringBootTest
        class ApplicationTest {
            @Test
            void contextLoads() {
                assertThat(true).isTrue();
            }
        }
        """;

    private final MustacheRenderer renderer;

    public Scaffolder() {
        this.renderer = new MustacheRenderer();
    }

    /**
     * Generates a complete Maven/Spring Boot project scaffold.
     *
     * @param projectName human-readable project name
     * @param description project description
     * @param outputDir   root directory where the project should be created
     * @return the generated project root path
     */
    public Path scaffold(String projectName, String description, Path outputDir) throws IOException {
        String artifactId = toArtifactId(projectName);
        String groupId = "com.example";
        String packageName = groupId + "." + artifactId.replace("-", "");
        String packagePath = packageName.replace('.', '/');

        Map<String, Object> model = Map.of(
            "projectName", projectName,
            "description", description,
            "groupId", groupId,
            "artifactId", artifactId,
            "packageName", packageName,
            "version", "0.0.1-SNAPSHOT"
        );

        Path projectRoot = outputDir.resolve(artifactId);
        Files.createDirectories(projectRoot);

        // pom.xml
        writeFile(projectRoot.resolve("pom.xml"), render(POM_TEMPLATE, model));

        // src/main/java/.../Application.java
        Path mainJava = projectRoot.resolve("src/main/java").resolve(packagePath);
        Files.createDirectories(mainJava);
        writeFile(mainJava.resolve("Application.java"), render(APPLICATION_TEMPLATE, model));

        // ChatController.java
        writeFile(mainJava.resolve("ChatController.java"), render(CHAT_CONTROLLER_TEMPLATE, model));

        // MemoryConfig.java
        writeFile(mainJava.resolve("MemoryConfig.java"), render(MEMORY_CONFIG_TEMPLATE, model));

        // src/main/resources/templates/index.html
        Path templates = projectRoot.resolve("src/main/resources/templates");
        Files.createDirectories(templates);
        writeFile(templates.resolve("index.html"), render(INDEX_HTML_TEMPLATE, model));

        // src/main/resources/application.yml
        writeFile(projectRoot.resolve("src/main/resources/application.yml"),
            render(APPLICATION_YML_TEMPLATE, model));

        // src/test/java/.../ApplicationTest.java
        Path testJava = projectRoot.resolve("src/test/java").resolve(packagePath);
        Files.createDirectories(testJava);
        writeFile(testJava.resolve("ApplicationTest.java"), render(TEST_TEMPLATE, model));

        // .gitignore
        writeFile(projectRoot.resolve(".gitignore"), """
            target/
            *.class
            *.jar
            *.log
            .idea/
            *.iml
            """);

        return projectRoot;
    }

    private String render(String template, Map<String, Object> model) {
        return renderer.render(template, model);
    }

    private void writeFile(Path path, String content) throws IOException {
        Files.writeString(path, content);
    }

    private String toArtifactId(String name) {
        return name.toLowerCase()
            .replaceAll("[^a-z0-9]", "-")
            .replaceAll("-+", "-")
            .replaceAll("^-|-$", "");
    }
}
