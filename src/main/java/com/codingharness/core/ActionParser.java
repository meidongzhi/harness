package com.codingharness.core;

import com.codingharness.llm.LlmResponse;
import java.util.*;

public class ActionParser {
    public List<Action> parse(LlmResponse response) {
        if (response.toolCalls() == null || response.toolCalls().isEmpty()) {
            return List.of();
        }
        return response.toolCalls().stream()
            .map(tc -> new Action(tc.name(), tc.arguments()))
            .toList();
    }
}
