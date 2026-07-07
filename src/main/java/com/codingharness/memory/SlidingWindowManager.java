package com.codingharness.memory;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages a sliding window of conversation turns with overflow
 * summarization support.
 *
 * <p>When the total number of turns reaches a multiple of
 * {@code summaryThreshold}, the oldest turns (those outside the
 * sliding window) are offered for summarization. After the caller
 * processes them, {@link #clearSummarized(int)} removes them,
 * keeping total memory bounded.</p>
 */
public class SlidingWindowManager {
    private final List<TurnRecord> fullHistory = new ArrayList<>();
    private final int windowSize;
    private final int summaryThreshold;

    /**
     * @param windowSize        number of most-recent turns to retain in the window
     * @param summaryThreshold  trigger summarization every N turns
     */
    public SlidingWindowManager(int windowSize, int summaryThreshold) {
        this.windowSize = windowSize;
        this.summaryThreshold = summaryThreshold;
    }

    /** Adds a new turn to the conversation history. */
    public void addTurn(TurnRecord turn) { fullHistory.add(turn); }

    /**
     * Returns the most recent {@code windowSize} turns (the active window).
     *
     * @return an unmodifiable snapshot of the window turns
     */
    public List<TurnRecord> getWindowTurns() {
        int from = Math.max(0, fullHistory.size() - windowSize);
        return List.copyOf(fullHistory.subList(from, fullHistory.size()));
    }

    /** Returns a snapshot of the entire conversation history. */
    public List<TurnRecord> getFullHistory() { return List.copyOf(fullHistory); }

    /** Returns the total number of turns stored. */
    public int totalTurns() { return fullHistory.size(); }

    /**
     * Returns {@code true} when the total turn count is a positive multiple
     * of {@code summaryThreshold}, indicating that a summarization should
     * be triggered.
     */
    public boolean needsSummarization() {
        return fullHistory.size() > 0 && fullHistory.size() % summaryThreshold == 0;
    }

    /**
     * Returns the turns that are candidates for summarization — those
     * older than the current window. If there are no turns outside the
     * window, returns an empty list.
     */
    public List<TurnRecord> getTurnsForSummarization() {
        if (fullHistory.size() <= windowSize) return List.of();
        int end = fullHistory.size() - windowSize;
        return List.copyOf(fullHistory.subList(0, end));
    }

    /**
     * Removes the oldest {@code count} turns from history after they have
     * been summarised. This frees memory while keeping the summary
     * elsewhere.
     */
    public void clearSummarized(int count) {
        if (count > 0 && count <= fullHistory.size()) {
            fullHistory.subList(0, count).clear();
        }
    }

    /** A single conversation turn. */
    public record TurnRecord(String role, String content, java.time.Instant timestamp) {
        /** Factory method that auto-assigns the current timestamp. */
        public static TurnRecord of(String role, String content) {
            return new TurnRecord(role, content, java.time.Instant.now());
        }
    }
}
