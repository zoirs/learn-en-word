package com.zoirs.learn_en_word.controller;

import com.zoirs.learn_en_word.api.dto.skyeng.Meaning;
import com.zoirs.learn_en_word.mapper.WordMapper;
import com.zoirs.learn_en_word.model.MeaningEntity;
import com.zoirs.learn_en_word.repository.MeaningRepository;
import com.zoirs.learn_en_word.service.ChatGPTService;
import com.zoirs.learn_en_word.service.DictionaryCacheService;
import com.zoirs.learn_en_word.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/word-suggestions")
@RequiredArgsConstructor
@Tag(name = "Word Suggestion", description = "API for getting word suggestions based on user's vocabulary")
public class WordSuggestionController {

    private static final Logger log = LoggerFactory.getLogger(WordSuggestionController.class);
    private final ChatGPTService chatGPTService;
    private final UserService userService;
    private final DictionaryCacheService dictionaryCacheService;
    private final MeaningRepository meaningRepository;

    // используется
    @PostMapping("/post")
    @Operation(summary = "Get word suggestions",
            description = "Get a list of suggested words to learn next based on known and currently learning words")
    public ResponseEntity<List<Meaning>> getWordSuggestions(
            @RequestBody State state
    ) {
        List<Integer> ids = new ArrayList<>();
        ids.addAll(state.getKnownWords());
        ids.addAll(state.getLearningWords());
        List<MeaningEntity> meanings = meaningRepository.findByExternalIdIn(ids);
        Set<String> knownWords = meanings.stream().filter(q -> state.getKnownWords().contains(q.getExternalId())).map(MeaningEntity::getText).collect(Collectors.toSet());
        Set<String> learningWords = meanings.stream().filter(q -> state.getLearningWords().contains(q.getExternalId())).map(MeaningEntity::getText).collect(Collectors.toSet());
        Set<String> suggestions = chatGPTService.suggestNewWords(knownWords, learningWords);
        if (suggestions == null || suggestions.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        List<Meaning> result = dictionaryCacheService.searchWords(suggestions);

        Set<Integer> newWords = result.stream().map(Meaning::getId).collect(Collectors.toSet());
        log.info("New words for userId {}: {}", state.getUserId(), newWords);
        userService.updateUserWords(state.getUserId(), state.getKnownWords(), state.getLearningWords(), newWords);
        return ResponseEntity.ok(result);
    }

}
