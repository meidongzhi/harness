package com.codingharness.memory;

import com.codingharness.llm.LlmProvider;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Unified runtime that orchestrates the full memory subsystem for a
 * single project conversation.
 *
 * <p>Manages sliding-window turn tracking, automatic summarization via
 * an LLM, storage of summaries in a {@link MemoryStore}, and assembly
 * of LLM-ready context strings that combine the current window with
 * relevant historical summaries.</p>
 *
 * <p>Typical usage:</p>
 * <pre>{@code
 * ProjectMemoryRuntime rt = new ProjectMemoryRuntime(20, 10, summaryStore, llm);
 * rt.addTurn("user", "What architecture should we use?");
 * rt.addTurn("assistant", "Microservices would work well.");
 * String ctx = rt.getContextForLLM();  // ready to feed into the next LLM call
 * }</pre>
 */
public class ProjectMemoryRuntime {
    private final SlidingWindowManager window;
    private final SummaryScheduler scheduler;
    private final MemoryStore summaryStore;
    private final LlmProvider llmProvider;

    /**
     * @param windowSize       number of recent turns to keep in the active window
     * @param threshold        summarization is triggered every {@code threshold} turns
     * @param summaryStore     where generated summaries are persisted
     * @param llmProvider      LLM backend used to generate summaries
     */
    public ProjectMemoryRuntime(int windowSize, int threshold,
                                MemoryStore summaryStore,
                                LlmProvider llmProvider) {
        if (windowSize <= 0) throw new IllegalArgumentException("windowSize must be positive");
        if (threshold <= 0) throw new IllegalArgumentException("threshold must be positive");
        Objects.requireNonNull(summaryStore, "summaryStore must not be null");
        Objects.requireNonNull(llmProvider, "llmProvider must not be null");
        this.window = new SlidingWindowManager(windowSize, threshold);
        this.scheduler = new SummaryScheduler();
        this.summaryStore = summaryStore;
        this.llmProvider = llmProvider;
    }

    /**
     * Adds a conversation turn. If the turn count triggers the
     * summarization threshold, a summary is generated and stored
     * automatically, and the summarised turns are removed from the
     * window history.
     *
     * @param role    "user" or "assistant"
     * @param content the turn text
     */
    public void addTurn(String role, String content) {
        window.addTurn(SlidingWindowManager.TurnRecord.of(role, content));
        if (window.needsSummarization()) {
            List<SlidingWindowManager.TurnRecord> turns = window.getTurnsForSummarization();
            ConversationSummary summary = scheduler.generateSummary(turns, llmProvider);
            summaryStore.save("summary:" + summary.id(),
                    summary.summary(),
                    Map.of("topics", String.join(",", summary.topics())));
            window.clearSummarized(turns.size());
        }
    }

    /**
     * Builds a formatted context string suitable for feeding into the
     * next LLM call. Includes the current window turns plus the most
     * recent historical summaries.
     *
     * @return a multi-section string with recent conversation and history
     */
    public String getContextForLLM() {
        StringBuilder sb = new StringBuilder();
        sb.append("## Recent Conversation\n");
        for (var turn : window.getWindowTurns()) {
            sb.append(turn.role()).append(": ").append(turn.content()).append("\n");
        }
        List<MemoryEntry> summaries = summaryStore.listRecent(5);
        if (!summaries.isEmpty()) {
            sb.append("\n## Relevant History\n");
            for (var entry : summaries) {
                sb.append("- ").append(entry.value()).append("\n");
            }
        }
        return sb.toString();
    }

    /** Returns the underlying sliding-window manager for inspection. */
    public SlidingWindowManager getWindow() { return window; }
}
