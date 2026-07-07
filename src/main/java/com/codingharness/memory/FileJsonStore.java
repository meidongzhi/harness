package com.codingharness.memory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

/**
 * Simple file-backed {@link MemoryStore} that persists all entries as a
 * single JSON array on disk.
 *
 * <p>Every mutation rewrites the entire file, so this implementation is
 * best suited for small datasets (hundreds of entries). For larger
 * workloads, prefer {@link SQLiteStore}.</p>
 *
 * <p>The JSON file is human-readable and can be inspected or edited
 * outside the application.</p>
 */
public class FileJsonStore implements MemoryStore {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(SerializationFeature.INDENT_OUTPUT);

    private final Path filePath;
    private final Map<String, MemoryEntry> store = new LinkedHashMap<>();
    private final List<MemoryEntry> history = new ArrayList<>();

    /**
     * Opens the given JSON file. If the file already exists its contents
     * are loaded; otherwise a new empty store is initialised.
     *
     * @param filePath path to the JSON persistence file
     */
    public FileJsonStore(Path filePath) {
        this.filePath = filePath;
        loadFromFile();
    }

    @Override
    public synchronized void save(String key, String value, Map<String, String> metadata) {
        MemoryEntry entry = new MemoryEntry(key, value, new HashMap<>(metadata), Instant.now());
        store.put(key, entry);
        history.removeIf(e -> e.key().equals(key));
        history.add(entry);
        writeToFile();
    }

    @Override
    public synchronized List<MemoryEntry> search(String query) {
        String lower = query.toLowerCase();
        return store.values().stream()
                .filter(e -> e.key().toLowerCase().contains(lower) || e.value().toLowerCase().contains(lower))
                .toList();
    }

    @Override
    public synchronized Optional<MemoryEntry> get(String key) {
        return Optional.ofNullable(store.get(key));
    }

    @Override
    public synchronized void delete(String key) {
        MemoryEntry removed = store.remove(key);
        if (removed != null) {
            history.removeIf(e -> e.key().equals(key));
            writeToFile();
        }
    }

    @Override
    public synchronized List<MemoryEntry> listRecent(int limit) {
        int size = history.size();
        int start = Math.max(0, size - limit);
        List<MemoryEntry> recent = new ArrayList<>(history.subList(start, size));
        Collections.reverse(recent);
        return recent;
    }

    private void loadFromFile() {
        if (!Files.exists(filePath)) {
            return;
        }
        try {
            String json = Files.readString(filePath);
            if (json.isBlank() || json.trim().equals("[]")) {
                return;
            }
            List<MemoryEntry> entries = MAPPER.readValue(json, new TypeReference<List<MemoryEntry>>() {});
            for (MemoryEntry entry : entries) {
                store.put(entry.key(), entry);
                history.add(entry);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load memory from " + filePath, e);
        }
    }

    private void writeToFile() {
        try {
            Files.createDirectories(filePath.getParent());
            List<MemoryEntry> entries = new ArrayList<>(store.values());
            MAPPER.writeValue(filePath.toFile(), entries);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write memory to " + filePath, e);
        }
    }
}
