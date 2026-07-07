package com.codingharness.tools;

import com.codingharness.core.ProjectContext;
import java.util.Map;

public interface Tool {
    String name();
    String description();
    Map<String, Object> parameters(); // JSON Schema
    ToolResult execute(Map<String, Object> args, ProjectContext ctx);
}
