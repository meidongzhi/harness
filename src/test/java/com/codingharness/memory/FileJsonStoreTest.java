package com.codingharness.memory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class FileJsonStoreTest {

    @Test
    void shouldSaveAndRetrieve(@TempDir Path tmpDir) {
        FileJsonStore store = new FileJsonStore(tmpDir.resolve("memory.json"));
        store.save("key1", "hello", Map.of("tag", "greeting"));
        assertThat(store.get("key1")).isPresent();
        assertThat(store.get("key1").get().value()).isEqualTo("hello");
        assertThat(store.get("key1").get().metadata()).containsEntry("tag", "greeting");
    }

    @Test
    void shouldSearchByKeyword(@TempDir Path tmpDir) {
        FileJsonStore store = new FileJsonStore(tmpDir.resolve("memory.json"));
        store.save("user:prefs", "dark mode", Map.of());
        store.save("project:test", "hello world", Map.of());
        assertThat(store.search("dark")).hasSize(1);
        assertThat(store.search("hello")).hasSize(1);
        assertThat(store.search("nonexistent")).isEmpty();
    }

    @Test
    void shouldDeleteAndReturnEmpty(@TempDir Path tmpDir) {
        FileJsonStore store = new FileJsonStore(tmpDir.resolve("memory.json"));
        store.save("temp", "x", Map.of());
        store.delete("temp");
        assertThat(store.get("temp")).isEmpty();
    }

    @Test
    void shouldPersistAcrossInstances(@TempDir Path tmpDir) {
        Path filePath = tmpDir.resolve("memory.json");
        FileJsonStore store1 = new FileJsonStore(filePath);
        store1.save("persist", "survives", Map.of("version", "1"));

        FileJsonStore store2 = new FileJsonStore(filePath);
        assertThat(store2.get("persist")).isPresent();
        assertThat(store2.get("persist").get().value()).isEqualTo("survives");
        assertThat(store2.get("persist").get().metadata()).containsEntry("version", "1");
    }

    @Test
    void shouldWriteValidJsonFile(@TempDir Path tmpDir) throws Exception {
        Path filePath = tmpDir.resolve("memory.json");
        FileJsonStore store = new FileJsonStore(filePath);
        store.save("entry1", "content1", Map.of("type", "note"));

        String json = Files.readString(filePath);
        assertThat(json).isNotBlank();
        assertThat(json).contains("entry1");
        assertThat(json).contains("content1");
        assertThat(json).contains("type");
        assertThat(json).contains("note");
    }

    @Test
    void shouldListRecentInOrder(@TempDir Path tmpDir) {
        FileJsonStore store = new FileJsonStore(tmpDir.resolve("memory.json"));
        store.save("a", "1", Map.of());
        store.save("b", "2", Map.of());
        store.save("c", "3", Map.of());
        assertThat(store.listRecent(2)).hasSize(2);
        assertThat(store.listRecent(2).get(0).key()).isEqualTo("c"); // newest first
    }
}
