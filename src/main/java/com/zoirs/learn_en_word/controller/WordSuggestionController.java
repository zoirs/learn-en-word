package com.zoirs.learn_en_word.controller;

import com.zoirs.learn_en_word.api.dto.skyeng.Meaning;
import com.zoirs.learn_en_word.api.dto.skyeng.Word;
import com.zoirs.learn_en_word.entity.UserWord;
import com.zoirs.learn_en_word.mapper.WordMapper;
import com.zoirs.learn_en_word.req.UserWordResponse;
import com.zoirs.learn_en_word.service.ChatGPTService;
import com.zoirs.learn_en_word.service.DictionaryCacheService;
import com.zoirs.learn_en_word.service.WordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/word-suggestions")
@RequiredArgsConstructor
@Tag(name = "Word Suggestion", description = "API for getting word suggestions based on user's vocabulary")
public class WordSuggestionController {

    private final ChatGPTService chatGPTService;
    private final WordService wordService;
    private final WordMapper wordMapper;
    private final DictionaryCacheService dictionaryCacheService;

    @PostMapping("/post")
    @Operation(summary = "Get word suggestions",
            description = "Get a list of suggested words to learn next based on known and currently learning words")
    public ResponseEntity<Set<Meaning>> getWordSuggestions(
            @RequestBody State state
    ) {
        Set<String> suggestions = chatGPTService.suggestNewWords(state.getKnownWords(), state.getLearningWords());
        if (suggestions == null || suggestions.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        Set<Meaning> result = new HashSet<>();
        for (String suggestion : suggestions) {
            List<Meaning> words = dictionaryCacheService.searchWords(suggestion);
            result.addAll(words);
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping
    @Operation(summary = "Get word suggestions",
              description = "Get a list of suggested words to learn next based on known and currently learning words")
    public ResponseEntity<Set<UserWordResponse>> getWordSuggestions(@RequestParam Long userId) {
        List<UserWord> userWords = wordService.getUserWords(userId);
        Set<String> learning = userWords.stream()
                .filter(q -> q.getStatus() == UserWord.LearningStatus.LEARNING)
                .map(q -> q.getWord().getText())
                .collect(Collectors.toSet());
        if (learning.size() > 2) {
            Set<UserWordResponse> result = userWords.stream()
                    .filter(q -> q.getStatus() == UserWord.LearningStatus.LEARNING)
                    .map(q -> new UserWordResponse(q, wordMapper.toDto(q.getMeaning())))
                    .collect(Collectors.toSet());
            return ResponseEntity.ok(result);
        }
        Set<String> mastered = userWords.stream()
                .filter(q -> q.getStatus() == UserWord.LearningStatus.MASTERED)
                .map(q -> q.getWord().getText())
                .collect(Collectors.toSet());
//        Set<String> suggestions = chatGPTService.suggestNewWords(mastered, learning);
//        if (suggestions == null || suggestions.isEmpty()) {
//            return ResponseEntity.noContent().build();
//        }
        wordService.ensureEnoughWordsForLearning(userId);

        userWords = wordService.getUserWords(userId);
        Set<UserWordResponse> result = userWords.stream()
                .filter(q -> q.getStatus() == UserWord.LearningStatus.LEARNING)
                .map(q -> new UserWordResponse(q, wordMapper.toDto(q.getMeaning())))
                .collect(Collectors.toSet());
        return ResponseEntity.ok(result);
    }
}
