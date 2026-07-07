package com.codingharness.tools;

import com.codingharness.llm.LlmRequest;
import java.util.*;

public class ToolRegistry {
    private final Map<String, Tool> tools = new LinkedHashMap<>();

    public void register(Tool tool) { tools.put(tool.name(), tool); }
    public Optional<Tool> get(String name) { return Optional.ofNullable(tools.get(name)); }
    public List<Tool> listAll() { return List.copyOf(tools.values()); }

    public List<LlmRequest.ToolDefinition> listForLLM() {
        return tools.values().stream()
            .map(t -> new LlmRequest.ToolDefinition(t.name(), t.description(), t.parameters()))
            .toList();
    }
}
