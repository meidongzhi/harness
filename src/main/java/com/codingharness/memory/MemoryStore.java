package com.codingharness.memory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Core interface for the Coding Harness memory subsystem.
 *
 * <p>Implementations provide a key-value store with metadata tagging,
 * full-text search across keys and values, and time-ordered retrieval.
 * Backends range from in-memory maps for ephemeral sessions to SQLite
 * and JSON files for persistent project memory.</p>
 */
public interface MemoryStore {

    /**
     * Saves (or overwrites) a memory entry with the given key, value,
     * and metadata. The entry is timestamped automatically.
     *
     * @param key      unique identifier for the entry
     * @param value    the content to store
     * @param metadata arbitrary string-to-string tags (tags, context, etc.)
     */
    void save(String key, String value, Map<String, String> metadata);

    /**
     * Searches all entries whose key or value contains the query string
     * (case-insensitive substring match).
     *
     * @param query the search term
     * @return matching entries, or an empty list if none found
     */
    List<MemoryEntry> search(String query);

    /**
     * Returns the entry for the exact key, if it exists.
     *
     * @param key the entry's key
     * @return the entry wrapped in an {@link Optional}, or {@link Optional#empty()}
     */
    Optional<MemoryEntry> get(String key);

    /**
     * Removes the entry identified by {@code key}. Does nothing if the
     * key does not exist.
     *
     * @param key the entry's key
     */
    void delete(String key);

    /**
     * Returns the {@code limit} most recently saved entries, newest first.
     *
     * @param limit maximum number of entries to return
     * @return the recent entries, or an empty list if the store is empty
     */
    List<MemoryEntry> listRecent(int limit);
}
