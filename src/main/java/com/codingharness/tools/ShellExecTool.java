package com.codingharness.tools;

import com.codingharness.core.ProjectContext;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ShellExecTool implements Tool {
    private static final long TIMEOUT_SECONDS = 30;

    @Override
    public String name() { return "shell_exec"; }

    @Override
    public String description() { return "Execute a shell command in the project directory"; }

    @Override
    public Map<String, Object> parameters() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "command", Map.of("type", "string", "description", "The command to execute"),
                "cwd", Map.of("type", "string", "description", "Working directory relative to project root (optional)")
            ),
            "required", java.util.List.of("command")
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> args, ProjectContext ctx) {
        String command = (String) args.get("command");
        String cwdParam = (String) args.get("cwd");

        if (command == null || command.isBlank()) {
            return ToolResult.failure("command parameter is required");
        }

        try {
            Path workingDir = ctx.projectRoot();
            if (cwdParam != null && !cwdParam.isBlank()) {
                workingDir = workingDir.resolve(cwdParam).normalize();
                if (!workingDir.startsWith(ctx.projectRoot().normalize())) {
                    return ToolResult.failure("cwd escapes project root: " + cwdParam);
                }
            }

            ProcessBuilder pb = new ProcessBuilder();
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                pb.command("cmd.exe", "/c", command);
            } else {
                pb.command("sh", "-c", command);
            }
            pb.directory(workingDir.toFile());
            pb.redirectErrorStream(true);

            Process process = pb.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append(System.lineSeparator());
                }
            }

            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return ToolResult.failure("command timed out after " + TIMEOUT_SECONDS + " seconds");
            }

            int exitCode = process.exitValue();
            String result = "Exit code: " + exitCode + System.lineSeparator() + output.toString();
            if (exitCode == 0) {
                return ToolResult.success(result);
            } else {
                return ToolResult.failure(result);
            }
        } catch (Exception e) {
            return ToolResult.failure("error executing command: " + e.getMessage());
        }
    }
}
