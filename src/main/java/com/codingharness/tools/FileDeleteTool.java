package com.codingharness.tools;

import com.codingharness.core.ProjectContext;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class FileDeleteTool implements Tool {
    @Override
    public String name() { return "file_delete"; }

    @Override
    public String description() { return "Delete a file from the project"; }

    @Override
    public Map<String, Object> parameters() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "path", Map.of("type", "string", "description", "File path relative to project root")
            ),
            "required", java.util.List.of("path")
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> args, ProjectContext ctx) {
        String path = (String) args.get("path");
        if (path == null || path.isBlank()) {
            return ToolResult.failure("path parameter is required");
        }
        try {
            Path resolved = ctx.projectRoot().resolve(path).normalize();
            if (!resolved.startsWith(ctx.projectRoot().normalize())) {
                return ToolResult.failure("path escapes project root: " + path);
            }
            if (!Files.exists(resolved)) {
                return ToolResult.failure("file not found: " + path);
            }
            Files.delete(resolved);
            return ToolResult.success(path);
        } catch (Exception e) {
            return ToolResult.failure("error deleting file: " + e.getMessage());
        }
    }
}
