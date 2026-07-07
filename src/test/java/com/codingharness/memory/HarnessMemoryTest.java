package com.codingharness.memory;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class HarnessMemoryTest {
    @Test
    void shouldRememberAndListProjects() {
        HarnessMemory mem = new HarnessMemory(new InMemoryStore());
        mem.rememberProject("p1", "Test Project", "A test");
        assertThat(mem.listProjects()).contains("p1");
    }

    @Test
    void shouldStoreAndRetrievePreferences() {
        HarnessMemory mem = new HarnessMemory(new InMemoryStore());
        mem.setProjectPreference("p1", "theme", "dark");
        assertThat(mem.getProjectPreferences("p1")).containsEntry("theme", "dark");
    }

    @Test
    void shouldRecordAndSearchDecisions() {
        HarnessMemory mem = new HarnessMemory(new InMemoryStore());
        mem.recordDecision("p1", "user wants dark UI", "use dark theme", "success");
        assertThat(mem.searchDecisions("p1", "dark")).hasSize(1);
    }
}
