package com.zoirs.learn_en_word.controller;

import com.zoirs.learn_en_word.api.dto.skyeng.Meaning;
import com.zoirs.learn_en_word.service.DictionaryCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/level-assessment")
@RequiredArgsConstructor
public class LevelAssessmentController {

    private final DictionaryCacheService dictionaryCacheService;

    private static final Set<String> a1 = Set.of("dog", "water", "book", "chair", "like", "apple", "school", "house", "run", "mother");
    private static final Set<String> a2 = Set.of("airport", "invite", "maybe", "homework", "job", "hungry", "holiday", "yesterday", "clean", "message");
    private static final Set<String> b1 = Set.of("advice", "career", "cancel", "customer", "explain", "prefer", "nervous", "borrow", "relationship", "almost");
    private static final Set<String> b2 = Set.of("efficient", "impact", "solution", "concern", "despite", "attempt", "policy", "reduce", "behaviour", "debate");
    private static final Set<String> c1 = Set.of("undermine", "coherent", "ambiguous", "nevertheless", "imply", "justify", "notion", "facilitate", "implement", "furthermore");

    @GetMapping("/initial-words")
    public ResponseEntity<Set<Meaning>> getWordSuggestions() {
        HashSet<Meaning> result = new HashSet<>();
        for (String word : a1) {
            result.addAll(getMeanings(word, 0));
        }
        for (String word : a2) {
            result.addAll(getMeanings(word, 1));
        }
        for (String word : b1) {
            result.addAll(getMeanings(word, 2));
        }
        for (String word : b2) {
            result.addAll(getMeanings(word, 3));
        }
        for (String word : c1) {
            result.addAll(getMeanings(word, 4));
        }

        return ResponseEntity.ok(result);
    }

    private List<Meaning> getMeanings(String word, int de) {
        List<Meaning> meanings = dictionaryCacheService.searchWords(word);
        if (!meanings.isEmpty()) {
            Meaning meaning = meanings.getFirst();
            if (meaning.getDifficultyLevel() == null) {
                meaning.setDifficultyLevel(de);
            }
        }
        return meanings;
    }

}
