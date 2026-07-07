package com.codingharness.core;

import com.codingharness.feedback.FeedbackResult;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class StopJudgeTest {
    @Test
    void shouldContinueOnFirstTurn() {
        StopJudge judge = new StopJudge(30, 3);
        var decision = judge.decide(List.of(), null, 0);
        assertThat(decision).isEqualTo(StopJudge.StopDecision.CONTINUE);
    }

    @Test
    void shouldReturnSuccessWhenTestsPass() {
        StopJudge judge = new StopJudge(30, 3);
        FeedbackResult feedback = new FeedbackResult(true, List.of(), List.of(), List.of());
        var decision = judge.decide(List.of("Tool test_run: all tests pass"), feedback, 5);
        assertThat(decision).isEqualTo(StopJudge.StopDecision.SUCCESS);
    }

    @Test
    void shouldReturnMaxTurnsAtLimit() {
        StopJudge judge = new StopJudge(5, 3);
        var decision = judge.decide(List.of(), null, 5);
        assertThat(decision).isEqualTo(StopJudge.StopDecision.MAX_TURNS);
    }

    @Test
    void shouldReturnNeedsHumanAfterMaxRetries() {
        StopJudge judge = new StopJudge(30, 2);
        FeedbackResult failure = new FeedbackResult(false,
            List.of(new FeedbackResult.TestFailure("Test.java", "test1", "fail")),
            List.of(), List.of());

        // First failure - should continue
        assertThat(judge.decide(List.of("some action"), failure, 0))
            .isEqualTo(StopJudge.StopDecision.CONTINUE);

        // Second failure - should need human (maxRetries=2, so 2 consecutive failures)
        assertThat(judge.decide(List.of("some action"), failure, 1))
            .isEqualTo(StopJudge.StopDecision.NEEDS_HUMAN);
    }

    @Test
    void shouldResetConsecutiveFailuresOnSuccess() {
        StopJudge judge = new StopJudge(30, 3);
        FeedbackResult failure = new FeedbackResult(false,
            List.of(new FeedbackResult.TestFailure("Test.java", "test1", "fail")),
            List.of(), List.of());
        FeedbackResult success = new FeedbackResult(true, List.of(), List.of(), List.of());

        // Fail once
        assertThat(judge.decide(List.of("action"), failure, 0))
            .isEqualTo(StopJudge.StopDecision.CONTINUE);

        // Succeed - resets
        assertThat(judge.decide(List.of("action"), success, 1))
            .isEqualTo(StopJudge.StopDecision.CONTINUE);

        // Fail again - should be 1st failure again (not 2nd)
        assertThat(judge.decide(List.of("action"), failure, 2))
            .isEqualTo(StopJudge.StopDecision.CONTINUE);
    }

    @Test
    void shouldReturnMaxTurnsPastLimit() {
        StopJudge judge = new StopJudge(10, 3);
        var decision = judge.decide(List.of(), null, 15);
        assertThat(decision).isEqualTo(StopJudge.StopDecision.MAX_TURNS);
    }
}
