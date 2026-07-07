package com.codingharness.config;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {

    private final Path configPath;

    public ConfigManager(Path configPath) {
        this.configPath = configPath;
    }

    @SuppressWarnings("unchecked")
    public HarnessConfig load() {
        if (!Files.exists(configPath)) {
            return HarnessConfig.defaults();
        }

        try (InputStream in = Files.newInputStream(configPath)) {
            Yaml yaml = new Yaml();
            Map<String, Object> data = yaml.load(in);

            if (data == null) {
                return HarnessConfig.defaults();
            }

            int maxTurns = getInt(data, "max_turns", 30);
            String autoApproveLevel = getString(data, "auto_approve_level", "WARNING");
            String model = getString(data, "model", "deepseek-chat");
            String provider = getString(data, "provider", "deepseek");
            String baseUrl = getString(data, "base_url", "https://api.deepseek.com");
            String memoryBackend = getString(data, "memory_backend", "sqlite");
            String memoryDbPath = getString(data, "memory_db_path",
                System.getProperty("user.home") + "/.coding-harness/memory.db");
            List<String> shellWhitelist = getList(data, "shell_whitelist",
                List.of("mvn", "java", "npm"));

            return new HarnessConfig(maxTurns, autoApproveLevel, model, provider,
                baseUrl, memoryBackend, memoryDbPath, shellWhitelist);
        } catch (IOException e) {
            return HarnessConfig.defaults();
        }
    }

    public void save(HarnessConfig config) {
        try {
            Files.createDirectories(configPath.getParent());
        } catch (IOException e) {
            throw new RuntimeException("Failed to create config directory", e);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("max_turns", config.maxTurns());
        data.put("auto_approve_level", config.autoApproveLevel());
        data.put("model", config.model());
        data.put("provider", config.provider());
        data.put("base_url", config.baseUrl());
        data.put("memory_backend", config.memoryBackend());
        data.put("memory_db_path", config.memoryDbPath());
        data.put("shell_whitelist", config.shellWhitelist());

        try (OutputStreamWriter writer = new OutputStreamWriter(
                Files.newOutputStream(configPath), StandardCharsets.UTF_8)) {
            Yaml yaml = new Yaml();
            yaml.dump(data, writer);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save config", e);
        }
    }

    private int getInt(Map<String, Object> data, String key, int defaultVal) {
        Object val = data.get(key);
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        if (val instanceof String) {
            try {
                return Integer.parseInt((String) val);
            } catch (NumberFormatException e) {
                return defaultVal;
            }
        }
        return defaultVal;
    }

    private String getString(Map<String, Object> data, String key, String defaultVal) {
        Object val = data.get(key);
        if (val == null) {
            return defaultVal;
        }
        return val.toString();
    }

    @SuppressWarnings("unchecked")
    private List<String> getList(Map<String, Object> data, String key, List<String> defaultVal) {
        Object val = data.get(key);
        if (val instanceof List) {
            return (List<String>) val;
        }
        return defaultVal;
    }
}
