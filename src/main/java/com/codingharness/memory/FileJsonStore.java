package com.codingharness.memory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
        // Create the entry
        MemoryEntry entry = new MemoryEntry(key, value, new HashMap<>(metadata), Instant.now());
        // Write to disk FIRST
        Map<String, MemoryEntry> updated = new HashMap<>(store);
        updated.put(key, entry);
        writeAllEntries(updated.values());
        // Then update in-memory state
        store.put(key, entry);
        history.removeIf(e -> e.key().equals(key));
        history.add(entry);
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
        MemoryEntry removed = store.get(key);
        if (removed != null) {
            // Write to disk FIRST
            Map<String, MemoryEntry> updated = new HashMap<>(store);
            updated.remove(key);
            writeAllEntries(updated.values());
            // Then update in-memory state
            store.remove(key);
            history.removeIf(e -> e.key().equals(key));
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
            Path parent = filePath.getParent();
            if (parent == null) {
                parent = Path.of(".");
            }
            Files.createDirectories(parent);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create directories for " + filePath, e);
        }
        List<MemoryEntry> entries = new ArrayList<>(store.values());
        writeAllEntries(entries);
    }

    private void writeAllEntries(Collection<MemoryEntry> entries) {
        Path tmpFile = filePath.resolveSibling(filePath.getFileName() + ".tmp");
        try {
            MAPPER.writeValue(tmpFile.toFile(), entries);
            Files.move(tmpFile, filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            try { Files.deleteIfExists(tmpFile); } catch (IOException ignored) {}
            throw new java.io.UncheckedIOException("Failed to write entries to " + filePath, e);
        }
    }
}
