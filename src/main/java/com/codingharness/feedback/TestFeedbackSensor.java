package com.codingharness.feedback;

import com.codingharness.core.ProjectContext;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TestFeedbackSensor implements FeedbackSensor {

    private final String mvnCommand;

    public TestFeedbackSensor() {
        this("mvn");
    }

    public TestFeedbackSensor(String mvnCommand) {
        this.mvnCommand = mvnCommand;
    }

    @Override
    public FeedbackResult sense(ProjectContext ctx) {
        List<FeedbackResult.TestFailure> failures = new ArrayList<>();
        List<FeedbackResult.CompileError> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        boolean buildSuccess = false;
        int testsRun = 0;
        int testsFailed = 0;
        int testsError = 0;

        Process process = null;
        try {
            ProcessBuilder pb = new ProcessBuilder(mvnCommand, "test");
            pb.directory(ctx.projectRoot().toFile());
            pb.redirectErrorStream(true);

            process = pb.start();

            List<String> outputLines = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    outputLines.add(line);

                    // Look for BUILD SUCCESS / BUILD FAILURE
                    if (line.contains("BUILD SUCCESS")) {
                        buildSuccess = true;
                    } else if (line.contains("BUILD FAILURE")) {
                        buildSuccess = false;
                    }

                    // Parse test summary: "Tests run: N, Failures: N, Errors: N, Skipped: N"
                    if (line.contains("Tests run:")) {
                        testsRun = extractInt(line, "Tests run:");
                        testsFailed = extractInt(line, "Failures:");
                        testsError = extractInt(line, "Errors:");
                    }
                }
            }

            // Parse failure details from output
            for (String line : outputLines) {
                if (line.contains("<<< FAILURE!")) {
                    String testName = line.replaceAll(".*<<< FAILURE!", "").trim();
                    failures.add(new FeedbackResult.TestFailure("unknown", testName, line.trim()));
                }
            }

            boolean finished = process.waitFor(300, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new FeedbackResult(false, List.of(),
                    List.of(new FeedbackResult.CompileError("timeout", 0, "Build timed out after 300s")),
                    List.of());
            }

            if (!buildSuccess && testsRun == 0 && testsFailed == 0 && testsError == 0) {
                errors.add(new FeedbackResult.CompileError("pom.xml", 0, "Build failed with no test results"));
            }

        } catch (Exception e) {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
            errors.add(new FeedbackResult.CompileError("N/A", 0, "Sensor error: " + e.getMessage()));
            return new FeedbackResult(false, failures, errors, warnings);
        }

        boolean allPassed = buildSuccess && testsFailed == 0 && testsError == 0 && errors.isEmpty();

        return new FeedbackResult(allPassed, failures, errors, warnings);
    }

    private int extractInt(String line, String prefix) {
        int idx = line.indexOf(prefix);
        if (idx < 0) return 0;
        int start = idx + prefix.length();
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < line.length(); i++) {
            char c = line.charAt(i);
            if (Character.isDigit(c)) {
                sb.append(c);
            } else if (!sb.isEmpty()) {
                break;
            }
        }
        try {
            return sb.isEmpty() ? 0 : Integer.parseInt(sb.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
