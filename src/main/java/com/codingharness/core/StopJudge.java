package com.codingharness.core;

import com.codingharness.feedback.FeedbackResult;
import java.util.List;

public class StopJudge {
    public enum StopDecision { CONTINUE, SUCCESS, FAILED, NEEDS_HUMAN, MAX_TURNS }

    private final int maxTurns;
    private final int maxRetries;
    private int consecutiveFailures = 0;

    public StopJudge(int maxTurns, int maxRetries) {
        this.maxTurns = maxTurns;
        this.maxRetries = maxRetries;
    }

    public StopDecision decide(List<String> history, FeedbackResult feedback, int currentTurn) {
        if (currentTurn >= maxTurns) {
            return StopDecision.MAX_TURNS;
        }

        if (feedback == null) {
            return StopDecision.CONTINUE;
        }

        if (feedback.allPassed() && !history.isEmpty()) {
            String lastAction = history.get(history.size() - 1);
            if (lastAction.contains("test_run") || lastAction.contains("all tests pass")) {
                return StopDecision.SUCCESS;
            }
        }

        if (!feedback.allPassed()) {
            consecutiveFailures++;
            if (consecutiveFailures >= maxRetries) {
                return StopDecision.NEEDS_HUMAN;
            }
            return StopDecision.CONTINUE;
        }

        consecutiveFailures = 0;
        return StopDecision.CONTINUE;
    }

    public void resetFailures() { consecutiveFailures = 0; }
}
