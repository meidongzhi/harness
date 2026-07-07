package com.codingharness.tools;

import com.codingharness.core.ProjectContext;
import java.util.HashMap;
import java.util.Map;

public class MemorySaveTool implements Tool {
    @Override
    public String name() { return "memory_save"; }

    @Override
    public String description() { return "Save an entry to the harness memory"; }

    @Override
    public Map<String, Object> parameters() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "key", Map.of("type", "string", "description", "Unique key for the memory entry"),
                "value", Map.of("type", "string", "description", "Content to store"),
                "metadata", Map.of("type", "object", "description", "Optional metadata key-value pairs")
            ),
            "required", java.util.List.of("key", "value")
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public ToolResult execute(Map<String, Object> args, ProjectContext ctx) {
        String key = (String) args.get("key");
        String value = (String) args.get("value");

        if (key == null || key.isBlank()) {
            return ToolResult.failure("key parameter is required");
        }
        if (value == null || value.isBlank()) {
            return ToolResult.failure("value parameter is required");
        }

        Map<String, String> metadata = new HashMap<>();
        Object metaObj = args.get("metadata");
        if (metaObj instanceof Map) {
            Map<String, Object> rawMeta = (Map<String, Object>) metaObj;
            for (Map.Entry<String, Object> entry : rawMeta.entrySet()) {
                metadata.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }

        try {
            ctx.harnessMemory().getStore().save(key, value, metadata);
            return ToolResult.success("memory entry saved: " + key);
        } catch (Exception e) {
            return ToolResult.failure("error saving memory: " + e.getMessage());
        }
    }
}
