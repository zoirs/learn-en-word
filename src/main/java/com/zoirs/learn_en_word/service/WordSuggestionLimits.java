package com.zoirs.learn_en_word.service;

public final class WordSuggestionLimits {

    private WordSuggestionLimits() {
    }

    public static Limits forLearningWordsCount(int learningWordsCount) {
        if (learningWordsCount < 10) {
            return new Limits(new GroupLimits(1, 2, 3), new GroupLimits(2, 3, 4));
        }
        if (learningWordsCount < 25) {
            return new Limits(new GroupLimits(2, 2, 3), new GroupLimits(2, 3, 3));
        }
        if (learningWordsCount < 40) {
            return new Limits(new GroupLimits(2, 2, 2), new GroupLimits(3, 3, 3));
        }
        if (learningWordsCount < 55) {
            return new Limits(new GroupLimits(3, 2, 2), new GroupLimits(3, 3, 2));
        }
        return new Limits(new GroupLimits(3, 2, 1), new GroupLimits(4, 3, 2));
    }

    public record Limits(GroupLimits chatGpt, GroupLimits database) {
    }

    public record GroupLimits(int easier, int same, int harder) {
    }
}
