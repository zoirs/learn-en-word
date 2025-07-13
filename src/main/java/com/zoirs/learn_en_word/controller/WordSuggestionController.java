package com.zoirs.learn_en_word.controller;

import com.zoirs.learn_en_word.service.ChatGPTService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/word-suggestions")
@RequiredArgsConstructor
@Tag(name = "Word Suggestion", description = "API for getting word suggestions based on user's vocabulary")
public class WordSuggestionController {

    private final ChatGPTService chatGPTService;

    @PostMapping
    @Operation(summary = "Get word suggestions",
              description = "Get a list of suggested words to learn next based on known and currently learning words")
    public ResponseEntity<List<String>> getWordSuggestions(
            @RequestParam List<String> knownWords,
            @RequestParam List<String> learningWords
    ) {
        List<String> suggestions = chatGPTService.suggestNewWords(knownWords, learningWords);
        if (suggestions == null || suggestions.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(suggestions);
    }
}
