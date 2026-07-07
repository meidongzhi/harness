package com.codingharness.llm;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class DeepSeekProviderTest {
    @Test
    void shouldConstructWithApiKeyAndBaseUrl() {
        DeepSeekProvider provider = new DeepSeekProvider("sk-test-key", "https://api.deepseek.com");
        assertThat(provider.getName()).isEqualTo("deepseek");
    }

    @Test
    void shouldRejectNullApiKey() {
        assertThatThrownBy(() -> new DeepSeekProvider(null, "https://api.deepseek.com"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("apiKey");
    }

    @Test
    void shouldRejectBlankApiKey() {
        assertThatThrownBy(() -> new DeepSeekProvider("  ", "https://api.deepseek.com"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("apiKey");
    }
}
