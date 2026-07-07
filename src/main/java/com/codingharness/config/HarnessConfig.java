package com.codingharness.config;

import java.util.List;

public record HarnessConfig(
    int maxTurns,
    String autoApproveLevel,
    String model,
    String provider,
    String baseUrl,
    String memoryBackend,
    String memoryDbPath,
    List<String> shellWhitelist
) {
    public static HarnessConfig defaults() {
        return new HarnessConfig(
            30, "WARNING", "deepseek-chat", "deepseek",
            "https://api.deepseek.com", "sqlite",
            System.getProperty("user.home") + "/.coding-harness/memory.db",
            List.of("mvn", "java", "npm")
        );
    }
}
