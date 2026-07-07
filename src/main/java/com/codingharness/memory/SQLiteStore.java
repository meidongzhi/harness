package com.codingharness.memory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.sql.*;
import java.time.Instant;
import java.util.*;

/**
 * Persistent {@link MemoryStore} backed by a local SQLite database file.
 *
 * <p>Each entry is stored in a single {@code memories} table with columns
 * for key, value, metadata (JSON), and timestamp. The implementation is
 * thread-safe at the SQLite connection level via WAL mode.</p>
 */
public class SQLiteStore implements MemoryStore {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String url;

    /**
     * Opens (or creates) a SQLite database at the given path and ensures
     * the {@code memories} schema exists.
     *
     * @param dbPath filesystem path for the SQLite database file
     */
    public SQLiteStore(Path dbPath) {
        this.url = "jdbc:sqlite:" + dbPath.toAbsolutePath();
        initSchema();
    }

    private void initSchema() {
        String sql = "CREATE TABLE IF NOT EXISTS memories (" +
                "key TEXT PRIMARY KEY, " +
                "value TEXT, " +
                "metadata TEXT, " +
                "timestamp TEXT)";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize SQLite schema", e);
        }
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL");
        } catch (SQLException e) {
            System.err.println("WARNING: Failed to enable WAL mode: " + e.getMessage());
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url);
    }

    @Override
    public void save(String key, String value, Map<String, String> metadata) {
        String sql = "INSERT OR REPLACE INTO memories (key, value, metadata, timestamp) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.setString(3, toJson(metadata));
            ps.setString(4, Instant.now().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save entry: " + key, e);
        }
    }

    @Override
    public List<MemoryEntry> search(String query) {
        String sql = "SELECT key, value, metadata, timestamp FROM memories WHERE key LIKE ? OR value LIKE ?";
        String like = "%" + query + "%";
        List<MemoryEntry> results = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, like);
            ps.setString(2, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(rowToEntry(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to search: " + query, e);
        }
        return results;
    }

    @Override
    public Optional<MemoryEntry> get(String key) {
        String sql = "SELECT key, value, metadata, timestamp FROM memories WHERE key = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rowToEntry(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get entry: " + key, e);
        }
        return Optional.empty();
    }

    @Override
    public void delete(String key) {
        String sql = "DELETE FROM memories WHERE key = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete entry: " + key, e);
        }
    }

    @Override
    public List<MemoryEntry> listRecent(int limit) {
        String sql = "SELECT key, value, metadata, timestamp FROM memories ORDER BY timestamp DESC LIMIT ?";
        List<MemoryEntry> results = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(rowToEntry(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list recent entries", e);
        }
        return results;
    }

    private MemoryEntry rowToEntry(ResultSet rs) throws SQLException {
        String key = rs.getString("key");
        String value = rs.getString("value");
        Map<String, String> metadata = fromJson(rs.getString("metadata"));
        Instant timestamp = Instant.parse(rs.getString("timestamp"));
        return new MemoryEntry(key, value, metadata, timestamp);
    }

    private String toJson(Map<String, String> metadata) {
        try {
            return MAPPER.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize metadata", e);
        }
    }

    private Map<String, String> fromJson(String json) {
        if (json == null || json.isEmpty()) {
            return Map.of();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize metadata", e);
        }
    }
}
