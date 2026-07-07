package com.codingharness.llm;

import java.util.List;
import java.util.Map;

public record LlmResponse(
    String content,
    List<ToolCall> toolCalls,
    String finishReason,
    TokenUsage tokenUsage
) {
    public record ToolCall(String id, String name, Map<String, Object> arguments) {}
    public record TokenUsage(int promptTokens, int completionTokens, int totalTokens) {}
}
