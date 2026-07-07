package com.codingharness.memory;

import java.time.Instant;
import java.util.*;

/**
 * High-level memory API for coding harness projects.
 *
 * <p>Provides convenient methods for remembering projects, storing and
 * retrieving preferences, recording decisions with context and outcome,
 * and searching across all stored memories. Backed by any {@link MemoryStore}
 * implementation, so it works equally well with in-memory, SQLite, or
 * file-based storage.</p>
 */
public class HarnessMemory {
    private final MemoryStore store;

    /**
     * Creates a new HarnessMemory backed by the given store.
     *
     * @param store the underlying storage implementation
     */
    public HarnessMemory(MemoryStore store) { this.store = store; }

    /**
     * Registers a project in memory with basic metadata.
     *
     * @param projectId   unique project identifier
     * @param name        human-readable project name
     * @param description brief description of the project
     */
    public void rememberProject(String projectId, String name, String description) {
        store.save("project:" + projectId + ":meta",
            name,
            Map.of("description", description, "createdAt", Instant.now().toString()));
    }

    /**
     * Returns all preferences for the given project as a mutable map.
     *
     * @param projectId the project identifier
     * @return a copy of the preferences, or empty map if none stored
     */
    public Map<String, String> getProjectPreferences(String projectId) {
        Optional<MemoryEntry> entry = store.get("project:" + projectId + ":prefs");
        if (entry.isPresent()) {
            return new HashMap<>(entry.get().metadata());
        }
        return Map.of();
    }

    /**
     * Sets a single preference key-value pair for a project. If the
     * preference already exists it is overwritten; otherwise it is added.
     *
     * @param projectId the project identifier
     * @param key       preference name
     * @param value     preference value
     */
    public void setProjectPreference(String projectId, String key, String value) {
        Map<String, String> prefs = new HashMap<>(getProjectPreferences(projectId));
        prefs.put(key, value);
        store.save("project:" + projectId + ":prefs", "preferences", prefs);
    }

    /**
     * Records a decision made during a project, along with the context
     * that led to it and its outcome.
     *
     * @param projectId the project identifier
     * @param context   what prompted the decision
     * @param decision  what was decided
     * @param outcome   the result or expected outcome
     */
    public void recordDecision(String projectId, String context, String decision, String outcome) {
        String id = UUID.randomUUID().toString();
        store.save("project:" + projectId + ":decision:" + id,
            decision,
            Map.of("context", context, "outcome", outcome, "timestamp", Instant.now().toString()));
    }

    /**
     * Searches all decisions for a project whose decision text or context
     * matches the query (case-insensitive substring match).
     *
     * @param projectId the project identifier
     * @param query     the search term
     * @return list of matching decision maps with {@code decision} and {@code context} keys
     */
    public List<Map<String, String>> searchDecisions(String projectId, String query) {
        return store.search(query).stream()
            .filter(e -> e.key().startsWith("project:" + projectId + ":decision:"))
            .map(e -> Map.of("decision", e.value(), "context", e.metadata().getOrDefault("context", "")))
            .toList();
    }

    /**
     * Lists all known project IDs by scanning stored keys.
     *
     * @return unmodifiable set of project identifiers
     */
    public List<String> listProjects() {
        Set<String> projects = new HashSet<>();
        for (MemoryEntry e : store.search("project:")) {
            String[] parts = e.key().split(":");
            if (parts.length >= 2) projects.add(parts[1]);
        }
        return List.copyOf(projects);
    }

    /** Returns the underlying store for direct access when needed. */
    public MemoryStore getStore() { return store; }
}
