package com.zoirs.learn_en_word.controller;

import com.zoirs.learn_en_word.api.dto.skyeng.Meaning;
import com.zoirs.learn_en_word.service.DictionaryCacheService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/level-assessment")
@RequiredArgsConstructor
public class LevelAssessmentController {

    private static final Logger log = LoggerFactory.getLogger(LevelAssessmentController.class);
    private final DictionaryCacheService dictionaryCacheService;

    private static final int DEFAULT_ERROR_COUNT = 3;
    private static final int V2_WORD_COUNT_PER_LEVEL = 3;

    private static final Set<String> a1 = Set.of("dog", "water", "book", "chair", "pen", "apple", "school", "house", "run", "mother");
    private static final Set<String> a2 = Set.of("airport", "invite", "maybe", "homework", "job", "hungry", "holiday", "yesterday", "clean", "message");
    private static final Set<String> b1 = Set.of("advice", "career", "cancel", "customer", "explain", "prefer", "nervous", "borrow", "relationship", "almost");
    private static final Set<String> b2 = Set.of("efficient", "impact", "solution", "concern", "despite", "attempt", "policy", "reduce", "behaviour", "debate");
    private static final Set<String> c1 = Set.of("undermine", "coherent", "ambiguous", "nevertheless", "imply", "justify", "notion", "facilitate", "implement", "furthermore");
    private static final Set<String> c2 = Set.of("ubiquitous", "ephemeral", "idiosyncratic", "perfunctory", "obfuscate", "magnanimous", "fastidious", "equanimity", "circumspect", "intransigent");

    private static final List<Integer> a1Ids = List.of(185522, 192984, 113022, 40794, 134538, 226138, 1938, 103001, 196389, 47960);
    private static final List<Integer> a2Ids = List.of(218132, 27657, 162704, 159881, 145645, 45009, 194381, 236469, 66333, 96141);
    private static final List<Integer> b1Ids = List.of(188460, 87724, 55378, 5825, 158980, 191137, 71806, 202759, 32873, 87011);
    private static final List<Integer> b2Ids = List.of(61001, 81871, 227998, 146399, 184993, 112178, 156016, 16726, 187115, 151773);
    private static final List<Integer> c1Ids = List.of(24452, 62337, 19918, 122630, 104853, 36834, 36603, 190309, 134057, 144646);
    private static final List<Integer> c2Ids = List.of(181891, 30610, 31920, 13109, 138447, 207434, 77223, 175732, 151198, 183618);

    private static final List<Integer> initialWordIds = Stream.of(a1Ids, a2Ids, b1Ids, b2Ids, c1Ids, c2Ids)
            .flatMap(List::stream)
            .toList();

    private static final List<Integer> initialWordIdsV2 = Stream.of(a1Ids, a2Ids, b1Ids, b2Ids, c1Ids, c2Ids)
            .flatMap(ids -> ids.stream().limit(V2_WORD_COUNT_PER_LEVEL))
            .toList();

    //используется
    @GetMapping("/initial-words")
    public ResponseEntity<List<Meaning>> getWordSuggestions() {
        List<Integer> ids = initialWordIds;
        List<Meaning> meanings = dictionaryCacheService.getMeanings(ids);
        for (Meaning meaning : meanings) {
            if (a1.contains(meaning.getText())) {
                meaning.setDifficultyLevel(0);
            } else if (a2.contains(meaning.getText())) {
                meaning.setDifficultyLevel(1);
            } else if (b1.contains(meaning.getText())) {
                meaning.setDifficultyLevel(2);
            } else if (b2.contains(meaning.getText())) {
                meaning.setDifficultyLevel(3);
            } else if (c1.contains(meaning.getText())) {
                meaning.setDifficultyLevel(4);
            } else if (c2.contains(meaning.getText())) {
                meaning.setDifficultyLevel(5);
            }
        }
        return ResponseEntity.ok(meanings);
    }

    @GetMapping("/v2/initial-words")
    public ResponseEntity<InitialWordsResponse> getInitialWordsV2() {
        List<Meaning> meanings = dictionaryCacheService.getMeanings(initialWordIdsV2);
        for (Meaning meaning : meanings) {
            if (a1.contains(meaning.getText())) {
                meaning.setDifficultyLevel(0);
            } else if (a2.contains(meaning.getText())) {
                meaning.setDifficultyLevel(1);
            } else if (b1.contains(meaning.getText())) {
                meaning.setDifficultyLevel(2);
            } else if (b2.contains(meaning.getText())) {
                meaning.setDifficultyLevel(3);
            } else if (c1.contains(meaning.getText())) {
                meaning.setDifficultyLevel(4);
            } else if (c2.contains(meaning.getText())) {
                meaning.setDifficultyLevel(5);
            }
        }
        return ResponseEntity.ok(new InitialWordsResponse(meanings, DEFAULT_ERROR_COUNT));
    }

    public record InitialWordsResponse(List<Meaning> words, int errorCount) {
    }
}
