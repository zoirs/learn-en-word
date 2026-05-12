package com.zoirs.learn_en_word.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WordSuggestionLimitsTest {

    @Test
    void forLearningWordsCount_ReturnsConfiguredLimits() {
        assertLimits(0, 1, 2, 3, 2, 3, 4);
        assertLimits(9, 1, 2, 3, 2, 3, 4);
        assertLimits(10, 2, 2, 3, 2, 3, 3);
        assertLimits(24, 2, 2, 3, 2, 3, 3);
        assertLimits(25, 2, 2, 2, 3, 3, 3);
        assertLimits(39, 2, 2, 2, 3, 3, 3);
        assertLimits(40, 3, 2, 2, 3, 3, 2);
        assertLimits(54, 3, 2, 2, 3, 3, 2);
        assertLimits(55, 3, 2, 1, 4, 3, 2);
    }

    private void assertLimits(
            int learningWordsCount,
            int chatGptEasier,
            int chatGptSame,
            int chatGptHarder,
            int databaseEasier,
            int databaseSame,
            int databaseHarder
    ) {
        WordSuggestionLimits.Limits limits = WordSuggestionLimits.forLearningWordsCount(learningWordsCount);

        assertEquals(chatGptEasier, limits.chatGpt().easier());
        assertEquals(chatGptSame, limits.chatGpt().same());
        assertEquals(chatGptHarder, limits.chatGpt().harder());
        assertEquals(databaseEasier, limits.database().easier());
        assertEquals(databaseSame, limits.database().same());
        assertEquals(databaseHarder, limits.database().harder());
    }
}
