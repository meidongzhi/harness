package com.codingharness.credentials;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.*;

class CredentialStoreTest {
    @Test
    void shouldStoreAndRetrieveCredential(@TempDir Path tmpDir) {
        AesCredentialStore store = new AesCredentialStore(tmpDir, "test-master-pw");
        store.store("deepseek-api-key", "sk-1234567890abcdef");
        assertThat(store.exists("deepseek-api-key")).isTrue();
        assertThat(store.retrieve("deepseek-api-key")).hasValue("sk-1234567890abcdef");
    }

    @Test
    void shouldReturnEmptyForMissingKey(@TempDir Path tmpDir) {
        AesCredentialStore store = new AesCredentialStore(tmpDir, "test-master-pw");
        assertThat(store.exists("nonexistent")).isFalse();
        assertThat(store.retrieve("nonexistent")).isEmpty();
    }

    @Test
    void shouldDeleteCredential(@TempDir Path tmpDir) {
        AesCredentialStore store = new AesCredentialStore(tmpDir, "test-master-pw");
        store.store("test-key", "test-value");
        store.delete("test-key");
        assertThat(store.exists("test-key")).isFalse();
    }

    @Test
    void shouldMaskDisplay(@TempDir Path tmpDir) {
        AesCredentialStore store = new AesCredentialStore(tmpDir, "test-master-pw");
        store.store("api-key", "sk-1234567890abcdef1234567890");
        String masked = store.maskedDisplay("api-key");
        assertThat(masked).contains("***");
        assertThat(masked).doesNotContain("1234567890abcdef");
        assertThat(masked).endsWith("567890");
    }

    @Test
    void shouldNotLeakPlaintextInStorage(@TempDir Path tmpDir) throws Exception {
        AesCredentialStore store = new AesCredentialStore(tmpDir, "test-master-pw");
        store.store("secret", "my-plaintext-secret");
        Path storeFile = tmpDir.resolve("credentials.enc");
        byte[] rawBytes = java.nio.file.Files.readAllBytes(storeFile);
        String rawContent = new String(rawBytes, java.nio.charset.StandardCharsets.ISO_8859_1);
        assertThat(rawContent).doesNotContain("my-plaintext-secret");
    }
}
