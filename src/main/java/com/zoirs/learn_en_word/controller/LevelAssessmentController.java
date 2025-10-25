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

@RestController
@RequestMapping("/api/level-assessment")
@RequiredArgsConstructor
public class LevelAssessmentController {

    private static final Logger log = LoggerFactory.getLogger(LevelAssessmentController.class);
    private final DictionaryCacheService dictionaryCacheService;

    private static final Set<String> a1 = Set.of("dog", "water", "book", "chair", "like", "apple", "school", "house", "run", "mother");
    private static final Set<String> a2 = Set.of("airport", "invite", "maybe", "homework", "job", "hungry", "holiday", "yesterday", "clean", "message");
    private static final Set<String> b1 = Set.of("advice", "career", "cancel", "customer", "explain", "prefer", "nervous", "borrow", "relationship", "almost");
    private static final Set<String> b2 = Set.of("efficient", "impact", "solution", "concern", "despite", "attempt", "policy", "reduce", "behaviour", "debate");
    private static final Set<String> c1 = Set.of("undermine", "coherent", "ambiguous", "nevertheless", "imply", "justify", "notion", "facilitate", "implement", "furthermore");
    private static final Set<String> c2 = Set.of("ubiquitous", "ephemeral", "idiosyncratic", "perfunctory", "obfuscate", "magnanimous", "fastidious", "equanimity", "circumspect", "intransigent");

    //используется
    @GetMapping("/initial-words")
    public ResponseEntity<List<Meaning>> getWordSuggestions() {
        List<Integer> ids = List.of(181891, 62337, 202759, 144646, 24452, 158980, 122630, 134538, 27657, 159881, 96141, 30610, 162704, 1938, 104853, 218132, 66333, 151198, 227998, 191137, 184993, 77223, 196389, 233641, 134057, 87724, 188460, 185522, 31920, 112178, 13109, 236469, 183618, 5825, 61001, 207434, 138447, 81871, 19918, 194381, 45009, 55378, 16726, 103001, 226138, 40794, 192984, 146399, 151773, 87011, 36834, 190309, 187115, 32873, 145645, 156016, 175732, 36603, 113022, 71806);
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
}
