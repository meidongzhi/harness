package com.codingharness.core;

import com.codingharness.config.HarnessConfig;
import com.codingharness.memory.HarnessMemory;
import java.nio.file.Path;

public record ProjectContext(Path projectRoot, String projectName, java.time.Instant createdAt, HarnessMemory harnessMemory, HarnessConfig config) {
    public static ProjectContext create(String name, Path root, HarnessMemory memory, HarnessConfig config) {
        return new ProjectContext(root, name, java.time.Instant.now(), memory, config);
    }
}
