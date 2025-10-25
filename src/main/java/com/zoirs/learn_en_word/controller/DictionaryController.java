package com.zoirs.learn_en_word.controller;

import com.zoirs.learn_en_word.api.dto.skyeng.Meaning;
import com.zoirs.learn_en_word.api.dto.skyeng.Word;
import com.zoirs.learn_en_word.service.DictionaryCacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dictionary")
@RequiredArgsConstructor
@Tag(name = "Dictionary", description = "Dictionary API with caching")
public class DictionaryController {

    private final DictionaryCacheService dictionaryCacheService;

    //используется
    @GetMapping("/search/words")
    @Operation(summary = "Search for words in the dictionary")
    public ResponseEntity<List<Word>> findWords(@RequestParam String query) {
        List<Word> words = dictionaryCacheService.searchWords1(query);
        return ResponseEntity.ok(words);
    }

    // используется
    @GetMapping("/meanings")
    @Operation(summary = "Get meanings by their IDs")
    public ResponseEntity<List<Meaning>> getMeanings(@RequestParam String ids) {
        List<Meaning> meanings = dictionaryCacheService.getMeanings(ids);
        return ResponseEntity.ok(meanings);
    }
}
