package com.codingharness.memory;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class InMemoryStoreTest {
    @Test
    void shouldSaveAndRetrieve() {
        InMemoryStore store = new InMemoryStore();
        store.save("key1", "value1", Map.of("tag", "test"));
        assertThat(store.get("key1")).isPresent();
        assertThat(store.get("key1").get().value()).isEqualTo("value1");
    }

    @Test
    void shouldSearchByKeyword() {
        InMemoryStore store = new InMemoryStore();
        store.save("user:prefs", "dark mode", Map.of());
        store.save("project:test", "hello world", Map.of());
        assertThat(store.search("dark")).hasSize(1);
        assertThat(store.search("hello")).hasSize(1);
        assertThat(store.search("nonexistent")).isEmpty();
    }

    @Test
    void shouldDeleteAndReturnEmpty() {
        InMemoryStore store = new InMemoryStore();
        store.save("temp", "x", Map.of());
        store.delete("temp");
        assertThat(store.get("temp")).isEmpty();
    }

    @Test
    void shouldListRecentInOrder() {
        InMemoryStore store = new InMemoryStore();
        store.save("a", "1", Map.of());
        store.save("b", "2", Map.of());
        store.save("c", "3", Map.of());
        assertThat(store.listRecent(2)).hasSize(2);
        assertThat(store.listRecent(2).get(0).key()).isEqualTo("c"); // newest first
    }
}
