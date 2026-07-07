package com.codingharness.feedback;

import java.util.List;

public record FeedbackResult(
    boolean allPassed,
    List<TestFailure> failures,
    List<CompileError> errors,
    List<String> warnings
) {
    public record TestFailure(String file, String testName, String message) {}
    public record CompileError(String file, int line, String message) {}

    public static FeedbackResult allGood() { return new FeedbackResult(true, List.of(), List.of(), List.of()); }
}
