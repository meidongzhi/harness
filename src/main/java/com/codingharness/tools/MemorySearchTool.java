package com.codingharness.tools;

import com.codingharness.core.ProjectContext;
import com.codingharness.memory.MemoryEntry;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MemorySearchTool implements Tool {
    @Override
    public String name() { return "memory_search"; }

    @Override
    public String description() { return "Search the harness memory for relevant entries"; }

    @Override
    public Map<String, Object> parameters() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "query", Map.of("type", "string", "description", "Search query for memory entries"),
                "limit", Map.of("type", "integer", "description", "Maximum number of results (default 10)")
            ),
            "required", java.util.List.of("query")
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> args, ProjectContext ctx) {
        String query = (String) args.get("query");
        if (query == null || query.isBlank()) {
            return ToolResult.failure("query parameter is required");
        }

        int limit = 10;
        Object limitObj = args.get("limit");
        if (limitObj instanceof Number) {
            limit = ((Number) limitObj).intValue();
        }

        try {
            List<MemoryEntry> results = ctx.harnessMemory().getStore().search(query);
            if (results.isEmpty()) {
                return ToolResult.success("No memory entries found for query: " + query);
            }

            StringBuilder output = new StringBuilder();
            output.append("Found ").append(results.size()).append(" memory entries:\n\n");

            results.stream()
                .limit(limit)
                .forEach(entry -> {
                    output.append("--- ").append(entry.key()).append(" ---\n");
                    output.append(entry.value()).append("\n");
                    if (!entry.metadata().isEmpty()) {
                        output.append("Metadata: ").append(entry.metadata()).append("\n");
                    }
                    output.append("Timestamp: ").append(entry.timestamp()).append("\n\n");
                });

            return ToolResult.success(output.toString());
        } catch (Exception e) {
            return ToolResult.failure("error searching memory: " + e.getMessage());
        }
    }
}
