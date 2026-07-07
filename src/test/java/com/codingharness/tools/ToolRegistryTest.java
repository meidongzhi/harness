package com.codingharness.tools;

import com.codingharness.llm.LlmRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class ToolRegistryTest {
    private ToolRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new ToolRegistry();
    }

    @Test
    void shouldRegisterAndRetrieveTool() {
        registry.register(new FileReadTool());
        assertThat(registry.get("file_read")).isPresent();
        assertThat(registry.get("file_read").get().name()).isEqualTo("file_read");
    }

    @Test
    void shouldReturnEmptyForUnknownTool() {
        assertThat(registry.get("nonexistent")).isEmpty();
    }

    @Test
    void shouldListAllRegisteredTools() {
        registry.register(new FileReadTool());
        registry.register(new FileWriteTool());
        List<Tool> all = registry.listAll();
        assertThat(all).hasSize(2);
        assertThat(all).extracting(Tool::name).containsExactly("file_read", "file_write");
    }

    @Test
    void listForLLMShouldReturnToolDefinitions() {
        registry.register(new FileReadTool());
        registry.register(new FileWriteTool());

        List<LlmRequest.ToolDefinition> defs = registry.listForLLM();
        assertThat(defs).hasSize(2);
        assertThat(defs).extracting(LlmRequest.ToolDefinition::name)
            .containsExactly("file_read", "file_write");
        assertThat(defs).allSatisfy(def -> {
            assertThat(def.description()).isNotEmpty();
            assertThat(def.parameters()).isNotEmpty();
        });
    }

    @Test
    void listForLLMShouldReturnEmptyListWhenNoTools() {
        List<LlmRequest.ToolDefinition> defs = registry.listForLLM();
        assertThat(defs).isEmpty();
    }
}
