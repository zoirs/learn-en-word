package com.zoirs.learn_en_word.dto.text;

import java.util.List;

public record GeneratedTextResponse(
        String text,
        List<Integer> knownMeaningIds,
        List<Integer> learningMeaningIds
) {
}
