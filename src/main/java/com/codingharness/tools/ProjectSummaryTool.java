package com.codingharness.tools;

import com.codingharness.core.ProjectContext;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class ProjectSummaryTool implements Tool {
    @Override
    public String name() { return "project_summary"; }

    @Override
    public String description() { return "Generate a summary of the project structure and files"; }

    @Override
    public Map<String, Object> parameters() {
        return Map.of(
            "type", "object",
            "properties", Map.of()
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> args, ProjectContext ctx) {
        try {
            StringBuilder summary = new StringBuilder();
            summary.append("Project: ").append(ctx.projectName()).append("\n");
            summary.append("Root: ").append(ctx.projectRoot()).append("\n");
            summary.append("Created: ").append(ctx.createdAt()).append("\n");
            summary.append("\n");

            // Check for build files
            Path pomFile = ctx.projectRoot().resolve("pom.xml");
            if (Files.exists(pomFile)) {
                summary.append("Build system: Maven\n");
                String pom = Files.readString(pomFile);
                // Extract groupId and artifactId
                for (String line : pom.split("\\R")) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith("<groupId>") && !trimmed.contains("xmlns")) {
                        summary.append("  groupId: ")
                            .append(trimmed.replaceAll("<[^>]+>", "").trim()).append("\n");
                    }
                    if (trimmed.startsWith("<artifactId>")) {
                        summary.append("  artifactId: ")
                            .append(trimmed.replaceAll("<[^>]+>", "").trim()).append("\n");
                        break;
                    }
                }
            }

            Path packageJson = ctx.projectRoot().resolve("package.json");
            if (Files.exists(packageJson)) {
                summary.append("Build system: Node.js/npm\n");
            }

            // Count source files
            summary.append("\nSource files:\n");
            Path srcDir = ctx.projectRoot().resolve("src");
            if (Files.exists(srcDir)) {
                try (Stream<Path> stream = Files.walk(srcDir)) {
                    List<Path> files = stream.filter(Files::isRegularFile).toList();
                    long javaFiles = files.stream().filter(p -> p.toString().endsWith(".java")).count();
                    long pyFiles = files.stream().filter(p -> p.toString().endsWith(".py")).count();
                    long jsFiles = files.stream().filter(p -> p.toString().endsWith(".js")).count();
                    summary.append("  Java files: ").append(javaFiles).append("\n");
                    summary.append("  Python files: ").append(pyFiles).append("\n");
                    summary.append("  JavaScript files: ").append(jsFiles).append("\n");
                }
            }

            summary.append("\nProject summary generated successfully.");
            return ToolResult.success(summary.toString());
        } catch (Exception e) {
            return ToolResult.failure("error generating project summary: " + e.getMessage());
        }
    }
}
