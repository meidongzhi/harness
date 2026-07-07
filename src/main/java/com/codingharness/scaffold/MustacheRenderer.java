package com.codingharness.scaffold;

import java.util.Map;

public class MustacheRenderer {
    public String render(String templateContent, Map<String, Object> model) {
        // Simple string replacement template (avoid classpath issues with Mustache)
        String result = templateContent;
        for (var entry : model.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", String.valueOf(entry.getValue()));
        }
        return result;
    }
}
