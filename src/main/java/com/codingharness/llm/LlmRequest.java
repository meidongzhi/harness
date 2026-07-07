package com.codingharness.llm;

import java.util.List;
import java.util.Map;

public record LlmRequest(
    String model,
    List<Message> messages,
    List<ToolDefinition> tools,
    int maxTokens,
    double temperature
) {
    public record Message(String role, String content) {
        public static Message system(String content) { return new Message("system", content); }
        public static Message user(String content) { return new Message("user", content); }
        public static Message assistant(String content) { return new Message("assistant", content); }
    }

    public record ToolDefinition(String name, String description, Map<String, Object> parameters) {}
}
