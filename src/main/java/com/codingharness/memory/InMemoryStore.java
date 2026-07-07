package com.codingharness.memory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryStore implements MemoryStore {
    private final Map<String, MemoryEntry> store = new ConcurrentHashMap<>();
    private final List<MemoryEntry> history = new ArrayList<>();
    private final Object historyLock = new Object();

    @Override
    public void save(String key, String value, Map<String, String> metadata) {
        MemoryEntry entry = new MemoryEntry(key, value, new HashMap<>(metadata), java.time.Instant.now());
        synchronized (historyLock) {
            store.put(key, entry);
            history.add(entry);
        }
    }

    @Override
    public List<MemoryEntry> search(String query) {
        String lower = query.toLowerCase();
        return store.values().stream()
            .filter(e -> e.key().toLowerCase().contains(lower) || e.value().toLowerCase().contains(lower))
            .collect(Collectors.toList());
    }

    @Override
    public Optional<MemoryEntry> get(String key) {
        return Optional.ofNullable(store.get(key));
    }

    @Override
    public void delete(String key) {
        store.remove(key);
        synchronized (historyLock) {
            history.removeIf(e -> e.key().equals(key));
        }
    }

    @Override
    public List<MemoryEntry> listRecent(int limit) {
        synchronized (historyLock) {
            int size = history.size();
            int start = Math.max(0, size - limit);
            List<MemoryEntry> recent = new ArrayList<>(history.subList(start, size));
            java.util.Collections.reverse(recent);
            return recent;
        }
    }
}
