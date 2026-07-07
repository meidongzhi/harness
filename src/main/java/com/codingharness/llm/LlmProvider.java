package com.codingharness.llm;

public interface LlmProvider {
    LlmResponse complete(LlmRequest request);
    String getName();
}
