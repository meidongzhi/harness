package com.codingharness.memory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class SQLiteStoreTest {
    @Test
    void shouldPersistAcrossInstances(@TempDir Path tmpDir) {
        Path dbPath = tmpDir.resolve("test.db");
        SQLiteStore store1 = new SQLiteStore(dbPath);
        store1.save("persist", "survives", Map.of("a", "1"));

        SQLiteStore store2 = new SQLiteStore(dbPath);
        assertThat(store2.get("persist")).isPresent();
        assertThat(store2.get("persist").get().value()).isEqualTo("survives");
    }

    @Test
    void shouldSearch(@TempDir Path tmpDir) {
        SQLiteStore store = new SQLiteStore(tmpDir.resolve("test.db"));
        store.save("greeting", "hello world", Map.of());
        store.save("farewell", "goodbye", Map.of());
        assertThat(store.search("hello")).hasSize(1);
    }

    @Test
    void shouldDelete(@TempDir Path tmpDir) {
        SQLiteStore store = new SQLiteStore(tmpDir.resolve("test.db"));
        store.save("temp", "x", Map.of());
        store.delete("temp");
        assertThat(store.get("temp")).isEmpty();
    }
}
