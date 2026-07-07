package com.codingharness.memory;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class SlidingWindowManagerTest {
    @Test
    void shouldReturnLastNWindowTurns() {
        SlidingWindowManager swm = new SlidingWindowManager(20, 10);
        for (int i = 0; i < 30; i++) {
            swm.addTurn(SlidingWindowManager.TurnRecord.of("user", "msg" + i));
        }
        assertThat(swm.getWindowTurns()).hasSize(20);
        assertThat(swm.getFullHistory()).hasSize(30);
    }

    @Test
    void shouldDetectNeedForSummarization() {
        SlidingWindowManager swm = new SlidingWindowManager(20, 10);
        for (int i = 0; i < 30; i++) {
            swm.addTurn(SlidingWindowManager.TurnRecord.of("user", "msg" + i));
        }
        assertThat(swm.needsSummarization()).isTrue(); // 30 % 10 == 0
        assertThat(swm.getTurnsForSummarization()).hasSize(10); // 30 - 20 = 10 turns to summarize
    }

    @Test
    void shouldNotNeedSummarizationBeforeThreshold() {
        SlidingWindowManager swm = new SlidingWindowManager(20, 10);
        for (int i = 0; i < 5; i++) {
            swm.addTurn(SlidingWindowManager.TurnRecord.of("user", "msg" + i));
        }
        assertThat(swm.needsSummarization()).isFalse();
    }
}
