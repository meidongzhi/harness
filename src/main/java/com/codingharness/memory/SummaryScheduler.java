package com.codingharness.memory;

import com.codingharness.llm.LlmProvider;
import com.codingharness.llm.LlmRequest;
import com.codingharness.llm.LlmResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Generates compressed conversation summaries using an {@link LlmProvider}.
 *
 * <p>When the {@link SlidingWindowManager} signals that a summarization is
 * due, this scheduler constructs a prompt from the overflow turns and calls
 * the configured LLM. The result is wrapped in a {@link ConversationSummary}
 * record for persistent storage.</p>
 */
public class SummaryScheduler {

    /**
     * Generates a summary of the given conversation turns by calling the
     * LLM with a summarization prompt.
     *
     * @param turns      the turns to summarise
     * @param llmProvider the LLM backend to use
     * @return a summary record with the LLM's output
     */
    public ConversationSummary generateSummary(
            List<SlidingWindowManager.TurnRecord> turns,
            LlmProvider llmProvider) {

        String conversationText = turns.stream()
                .map(t -> t.role() + ": " + t.content())
                .collect(Collectors.joining("\n"));

        String prompt = "Summarize the following conversation in 2-3 sentences, "
                + "focusing on key topics and emotional tone:\n" + conversationText;

        LlmRequest request = new LlmRequest(
                "default",
                List.of(LlmRequest.Message.user(prompt)),
                List.of(),
                200,
                0.3
        );

        LlmResponse response = llmProvider.complete(request);

        String summary = response.content();
        List<String> topics = extractTopics(summary);

        return new ConversationSummary(
                UUID.randomUUID().toString(),
                summary,
                topics,
                Instant.now()
        );
    }

    /**
     * Extracts likely topics from the summary text by scanning for
     * capitalized words and key terms. This is a simple heuristic;
     * a production implementation might use the LLM directly for
     * topic extraction.
     */
    private List<String> extractTopics(String text) {
        return List.of(text.split("\\s+")).stream()
                .filter(w -> w.length() > 3 && Character.isUpperCase(w.charAt(0)))
                .map(w -> w.replaceAll("[^a-zA-Z]", ""))
                .distinct()
                .limit(5)
                .toList();
    }
}
