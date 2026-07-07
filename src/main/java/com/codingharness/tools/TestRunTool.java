package com.codingharness.tools;

import com.codingharness.core.ProjectContext;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class TestRunTool implements Tool {
    private static final long TIMEOUT_SECONDS = 120;

    @Override
    public String name() { return "test_run"; }

    @Override
    public String description() { return "Run tests in the project using Maven"; }

    @Override
    public Map<String, Object> parameters() {
        return Map.of(
            "type", "object",
            "properties", Map.of()
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> args, ProjectContext ctx) {
        Process process = null;
        try {
            ProcessBuilder pb = new ProcessBuilder();
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                pb.command("cmd.exe", "/c", "mvn test -q");
            } else {
                pb.command("sh", "-c", "mvn test -q");
            }
            pb.directory(ctx.projectRoot().toFile());
            pb.redirectErrorStream(true);

            process = pb.start();
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
                return ToolResult.failure("test run timed out after " + TIMEOUT_SECONDS + " seconds");
            }

            int exitCode = process.exitValue();
            String outputStr = output.toString();

            // Parse test results
            long testsRun = 0;
            long failures = 0;
            long errors = 0;
            long skipped = 0;

            for (String line : outputStr.split("\\R")) {
                if (line.contains("Tests run:")) {
                    String[] parts = line.split(",");
                    for (String part : parts) {
                        part = part.trim();
                        if (part.startsWith("Tests run:")) {
                            testsRun = Long.parseLong(part.replace("Tests run:", "").trim());
                        } else if (part.startsWith("Failures:")) {
                            failures = Long.parseLong(part.replace("Failures:", "").trim());
                        } else if (part.startsWith("Errors:")) {
                            errors = Long.parseLong(part.replace("Errors:", "").trim());
                        } else if (part.startsWith("Skipped:")) {
                            skipped = Long.parseLong(part.replace("Skipped:", "").trim());
                        }
                    }
                }
            }

            StringBuilder summary = new StringBuilder();
            summary.append("Test Results:\n");
            summary.append("Tests run: ").append(testsRun).append("\n");
            summary.append("Failures: ").append(failures).append("\n");
            summary.append("Errors: ").append(errors).append("\n");
            summary.append("Skipped: ").append(skipped).append("\n");
            summary.append("Exit code: ").append(exitCode).append("\n");
            summary.append("\nFull output:\n").append(outputStr);

            if (exitCode == 0 && failures == 0 && errors == 0) {
                return ToolResult.success(summary.toString());
            } else {
                return ToolResult.failure(summary.toString());
            }
        } catch (Exception e) {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
            return ToolResult.failure("error running tests: " + e.getMessage());
        }
    }
}
