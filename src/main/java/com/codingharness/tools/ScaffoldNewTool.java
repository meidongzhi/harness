package com.codingharness.tools;

import com.codingharness.core.ProjectContext;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class ScaffoldNewTool implements Tool {
    @Override
    public String name() { return "scaffold_new"; }

    @Override
    public String description() { return "Scaffold a new project with a basic structure"; }

    @Override
    public Map<String, Object> parameters() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "name", Map.of("type", "string", "description", "Project name"),
                "type", Map.of("type", "string", "description", "Project type (java-maven, node, python)")
            ),
            "required", java.util.List.of("name", "type")
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> args, ProjectContext ctx) {
        String name = (String) args.get("name");
        String type = (String) args.get("type");

        if (name == null || name.isBlank()) {
            return ToolResult.failure("name parameter is required");
        }
        if (type == null || type.isBlank()) {
            return ToolResult.failure("type parameter is required");
        }

        try {
            Path projectDir = ctx.projectRoot().resolve(name).normalize();
            if (!projectDir.startsWith(ctx.projectRoot().normalize())) {
                return ToolResult.failure("project name escapes root: " + name);
            }
            if (Files.exists(projectDir)) {
                return ToolResult.failure("project directory already exists: " + name);
            }

            Files.createDirectories(projectDir);

            switch (type.toLowerCase()) {
                case "java-maven":
                    Files.createDirectories(projectDir.resolve("src/main/java"));
                    Files.createDirectories(projectDir.resolve("src/test/java"));
                    String pomXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n"
                        + "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                        + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"
                        + "    <modelVersion>4.0.0</modelVersion>\n"
                        + "    <groupId>com.example</groupId>\n"
                        + "    <artifactId>" + name + "</artifactId>\n"
                        + "    <version>1.0.0</version>\n"
                        + "</project>\n";
                    Files.writeString(projectDir.resolve("pom.xml"), pomXml);
                    break;
                case "node":
                    Files.createDirectories(projectDir.resolve("src"));
                    String packageJson = "{\n"
                        + "  \"name\": \"" + name + "\",\n"
                        + "  \"version\": \"1.0.0\",\n"
                        + "  \"main\": \"src/index.js\"\n"
                        + "}\n";
                    Files.writeString(projectDir.resolve("package.json"), packageJson);
                    Files.writeString(projectDir.resolve("src/index.js"),
                        "console.log('Hello from " + name + "');\n");
                    break;
                case "python":
                    Files.createDirectories(projectDir.resolve(name.replace("-", "_")));
                    Files.writeString(projectDir.resolve("requirements.txt"), "");
                    Files.writeString(projectDir.resolve(name.replace("-", "_") + "/__init__.py"), "");
                    Files.writeString(projectDir.resolve(name.replace("-", "_") + "/main.py"),
                        "def main():\n    print('Hello from " + name + "')\n\nif __name__ == '__main__':\n    main()\n");
                    break;
                default:
                    return ToolResult.failure("unsupported project type: " + type
                        + ". Supported: java-maven, node, python");
            }

            return ToolResult.success("Scaffolded " + type + " project: " + name);
        } catch (Exception e) {
            return ToolResult.failure("error scaffolding project: " + e.getMessage());
        }
    }
}
