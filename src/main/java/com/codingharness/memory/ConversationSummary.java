package com.codingharness.memory;

import java.time.Instant;
import java.util.List;

/**
 * A compressed representation of one or more conversation turns produced
 * by the {@link SummaryScheduler}.
 *
 * @param id        unique identifier for this summary
 * @param summary   the condensed text
 * @param topics    extracted keywords or themes
 * @param createdAt when the summary was generated
 */
public record ConversationSummary(String id, String summary, List<String> topics, Instant createdAt) {}
