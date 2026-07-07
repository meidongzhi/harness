package com.codingharness.memory;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public record MemoryEntry(String key, String value, Map<String, String> metadata, Instant timestamp) {
    public static MemoryEntry of(String key, String value) {
        return new MemoryEntry(key, value, new HashMap<>(), Instant.now());
    }
}
