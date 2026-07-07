package com.codingharness.memory;

import java.util.List;
import java.util.Optional;

public interface MemoryStore {
    void save(String key, String value, java.util.Map<String, String> metadata);
    List<MemoryEntry> search(String query);
    Optional<MemoryEntry> get(String key);
    void delete(String key);
    List<MemoryEntry> listRecent(int limit);
}
