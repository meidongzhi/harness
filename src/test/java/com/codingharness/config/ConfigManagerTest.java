package com.codingharness.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.*;

class ConfigManagerTest {
    @Test
    void shouldReturnDefaultsWhenConfigFileDoesNotExist() {
        ConfigManager mgr = new ConfigManager(Path.of("/nonexistent/config.yml"));
        HarnessConfig config = mgr.load();
        assertThat(config.maxTurns()).isEqualTo(30);
        assertThat(config.model()).isEqualTo("deepseek-chat");
    }

    @Test
    void shouldSaveAndLoadConfig(@TempDir Path tmpDir) {
        Path configPath = tmpDir.resolve("config.yml");
        ConfigManager mgr = new ConfigManager(configPath);
        mgr.save(HarnessConfig.defaults());
        ConfigManager mgr2 = new ConfigManager(configPath);
        HarnessConfig loaded = mgr2.load();
        assertThat(loaded.maxTurns()).isEqualTo(30);
    }

    @Test
    void shouldLoadCustomValues(@TempDir Path tmpDir) throws Exception {
        Path configPath = tmpDir.resolve("config.yml");
        String yaml = """
            max_turns: 20
            auto_approve_level: SAFE
            model: deepseek-chat
            provider: deepseek
            base_url: https://api.deepseek.com
            memory_backend: inmemory
            memory_db_path: /tmp/memory.db
            shell_whitelist:
              - mvn
              - java
            """;
        java.nio.file.Files.writeString(configPath, yaml);
        ConfigManager mgr = new ConfigManager(configPath);
        HarnessConfig config = mgr.load();
        assertThat(config.maxTurns()).isEqualTo(20);
        assertThat(config.autoApproveLevel()).isEqualTo("SAFE");
        assertThat(config.memoryBackend()).isEqualTo("inmemory");
    }
}
