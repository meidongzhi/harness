package com.codingharness.tools;

import com.codingharness.core.ProjectContext;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class FileWriteTool implements Tool {
    @Override
    public String name() { return "file_write"; }

    @Override
    public String description() { return "Write content to a file in the project"; }

    @Override
    public Map<String, Object> parameters() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "path", Map.of("type", "string", "description", "File path relative to project root"),
                "content", Map.of("type", "string", "description", "Content to write")
            ),
            "required", java.util.List.of("path", "content")
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> args, ProjectContext ctx) {
        String path = (String) args.get("path");
        String content = (String) args.get("content");
        if (path == null || path.isBlank()) {
            return ToolResult.failure("path parameter is required");
        }
        if (content == null) {
            return ToolResult.failure("content parameter is required");
        }
        try {
            Path resolved = ctx.projectRoot().resolve(path).normalize();
            if (!resolved.startsWith(ctx.projectRoot().normalize())) {
                return ToolResult.failure("path escapes project root: " + path);
            }
            Path parent = resolved.getParent();
            if (parent == null) {
                return ToolResult.failure("Cannot operate on root path");
            }
            Files.createDirectories(parent);
            Files.writeString(resolved, content);
            return ToolResult.success(path);
        } catch (Exception e) {
            return ToolResult.failure("error writing file: " + e.getMessage());
        }
    }
}
