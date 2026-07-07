package com.codingharness.tools;

public record ToolResult(boolean success, String output, String error) {
    public static ToolResult success(String output) { return new ToolResult(true, output, ""); }
    public static ToolResult failure(String error) { return new ToolResult(false, "", error); }
}
