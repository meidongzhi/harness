package com.codingharness.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DeepSeekProvider implements LlmProvider {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final ObjectMapper mapper = new ObjectMapper();

    private final String apiKey;
    private final String baseUrl;
    private final OkHttpClient client;

    public DeepSeekProvider(String apiKey, String baseUrl) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("apiKey must not be null or blank");
        }
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public String getName() {
        return "deepseek";
    }

    @Override
    public LlmResponse complete(LlmRequest llmRequest) {
        try {
            Map<String, Object> requestBody = buildRequestBody(llmRequest);
            String json = mapper.writeValueAsString(requestBody);

            Request httpRequest = new Request.Builder()
                    .url(baseUrl + "/v1/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(json, JSON))
                    .build();

            try (Response httpResponse = client.newCall(httpRequest).execute()) {
                if (!httpResponse.isSuccessful()) {
                    String errorBody = httpResponse.body() != null ? httpResponse.body().string() : "no body";
                    throw new RuntimeException("DeepSeek API error: " + httpResponse.code() + " - " + errorBody);
                }
                String responseBody = httpResponse.body().string();
                return parseResponse(responseBody);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize request or deserialize response", e);
        } catch (IOException e) {
            throw new RuntimeException("IO error calling DeepSeek API", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildRequestBody(LlmRequest llmRequest) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", llmRequest.model());

        List<Map<String, String>> messagesList = new ArrayList<>();
        for (LlmRequest.Message msg : llmRequest.messages()) {
            Map<String, String> msgMap = new HashMap<>();
            msgMap.put("role", msg.role());
            msgMap.put("content", msg.content());
            messagesList.add(msgMap);
        }
        body.put("messages", messagesList);

        if (llmRequest.tools() != null && !llmRequest.tools().isEmpty()) {
            List<Map<String, Object>> toolsList = new ArrayList<>();
            for (LlmRequest.ToolDefinition tool : llmRequest.tools()) {
                Map<String, Object> toolMap = new HashMap<>();
                toolMap.put("type", "function");
                Map<String, Object> functionMap = new HashMap<>();
                functionMap.put("name", tool.name());
                functionMap.put("description", tool.description());
                functionMap.put("parameters", tool.parameters());
                toolMap.put("function", functionMap);
                toolsList.add(toolMap);
            }
            body.put("tools", toolsList);
        }

        body.put("max_tokens", llmRequest.maxTokens());
        body.put("temperature", llmRequest.temperature());

        return body;
    }

    @SuppressWarnings("unchecked")
    private LlmResponse parseResponse(String responseBody) throws JsonProcessingException {
        Map<String, Object> responseMap = mapper.readValue(responseBody, Map.class);

        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
        Map<String, Object> choice = choices.get(0);
        Map<String, Object> message = (Map<String, Object>) choice.get("message");

        String content = (String) message.get("content");
        String finishReason = (String) choice.get("finish_reason");

        List<LlmResponse.ToolCall> toolCalls = new ArrayList<>();
        List<Map<String, Object>> rawToolCalls = (List<Map<String, Object>>) message.get("tool_calls");
        if (rawToolCalls != null) {
            for (Map<String, Object> rawToolCall : rawToolCalls) {
                String id = (String) rawToolCall.get("id");
                Map<String, Object> function = (Map<String, Object>) rawToolCall.get("function");
                String name = (String) function.get("name");
                Map<String, Object> arguments;
                try {
                    arguments = mapper.readValue(
                            (String) function.get("arguments"), Map.class);
                } catch (JsonProcessingException e) {
                    arguments = Map.of();
                }
                toolCalls.add(new LlmResponse.ToolCall(id, name, arguments));
            }
        }

        Map<String, Object> usage = (Map<String, Object>) responseMap.get("usage");
        LlmResponse.TokenUsage tokenUsage;
        if (usage != null) {
            tokenUsage = new LlmResponse.TokenUsage(
                    ((Number) usage.get("prompt_tokens")).intValue(),
                    ((Number) usage.get("completion_tokens")).intValue(),
                    ((Number) usage.get("total_tokens")).intValue()
            );
        } else {
            tokenUsage = new LlmResponse.TokenUsage(0, 0, 0);
        }

        return new LlmResponse(content, toolCalls, finishReason, tokenUsage);
    }
}
