package com.codingharness.core;

import com.codingharness.feedback.FeedbackResult;
import com.codingharness.feedback.FeedbackSensor;
import com.codingharness.guard.GuardChain;
import com.codingharness.guard.HitlStateMachine;
import com.codingharness.llm.LlmProvider;
import com.codingharness.tools.*;
import java.util.*;

public class AgentLoopImpl {
    private final LlmProvider llm;
    private final ToolRegistry tools;
    private final GuardChain guardChain;
    private final FeedbackSensor feedbackSensor;
    private final ContextBuilder contextBuilder;
    private final StopJudge stopJudge;
    private final int maxTurns;

    public AgentLoopImpl(LlmProvider llm, ToolRegistry tools, GuardChain guardChain,
                         FeedbackSensor feedbackSensor, int maxTurns, int maxRetries) {
        this.llm = llm;
        this.tools = tools;
        this.guardChain = guardChain;
        this.feedbackSensor = feedbackSensor;
        this.contextBuilder = new ContextBuilder();
        this.stopJudge = new StopJudge(maxTurns, maxRetries);
        this.maxTurns = maxTurns;
    }

    public enum LoopResult { SUCCESS, FAILED, NEEDS_HUMAN, MAX_TURNS }

    public LoopResult run(ProjectContext ctx) {
        List<String> history = new ArrayList<>();
        FeedbackResult feedback = null;
        HitlStateMachine hitl = new HitlStateMachine();

        for (int turn = 0; turn < maxTurns; turn++) {
            // 1. Build context
            var request = contextBuilder.build(tools, ctx, history, feedback);

            // 2. Call LLM
            var response = llm.complete(request);
            history.add("LLM: " + truncate(response.content(), 200));

            // 3. Parse actions
            List<Action> actions = new ActionParser().parse(response);
            if (actions.isEmpty()) {
                history.add("Agent: " + truncate(response.content(), 500));
            }

            // 4. Execute each action through guard chain
            for (Action action : actions) {
                var guardResult = guardChain.check(action, ctx);
                if (!guardResult.allowed()) {
                    history.add("BLOCKED: " + action.type() + " - " + guardResult.reason());
                    continue;
                }

                // Execute tool
                Tool tool = tools.get(action.type()).orElse(null);
                if (tool == null) {
                    history.add("Unknown tool: " + action.type());
                    continue;
                }

                ToolResult result = tool.execute(action.parameters(), ctx);
                history.add("Tool " + action.type() + ": " + truncate(result.output(), 300));
            }

            // 5. Get feedback
            feedback = feedbackSensor.sense(ctx);

            // 6. Stop judge
            var decision = stopJudge.decide(history, feedback, turn);
            switch (decision) {
                case SUCCESS -> { return LoopResult.SUCCESS; }
                case FAILED, MAX_TURNS -> { return LoopResult.MAX_TURNS; }
                case NEEDS_HUMAN -> { return LoopResult.NEEDS_HUMAN; }
                case CONTINUE -> { /* keep going */ }
            }
        }
        return LoopResult.MAX_TURNS;
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }
}
