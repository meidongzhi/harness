package com.codingharness.llm;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class MockLlmProvider implements LlmProvider {
    private final Queue<LlmResponse> responseQueue;
    private final LlmResponse lastResponse;

    public MockLlmProvider(List<LlmResponse> scriptedResponses) {
        if (scriptedResponses == null || scriptedResponses.isEmpty()) {
            throw new IllegalArgumentException("scriptedResponses must not be empty");
        }
        this.responseQueue = new LinkedBlockingQueue<>(scriptedResponses);
        this.lastResponse = scriptedResponses.get(scriptedResponses.size() - 1);
    }

    @Override
    public String getName() { return "mock"; }

    @Override
    public LlmResponse complete(LlmRequest request) {
        LlmResponse response = responseQueue.poll();
        return response != null ? response : lastResponse;
    }

    public int remainingResponses() { return responseQueue.size(); }
}
