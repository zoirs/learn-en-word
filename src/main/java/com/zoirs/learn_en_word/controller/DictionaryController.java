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

    @GetMapping("/search")
    @Operation(summary = "Search for words in the dictionary")
    public ResponseEntity<List<Word>> searchWords(@RequestParam String query) {
        List<Word> words = dictionaryCacheService.searchWords(query);
        return ResponseEntity.ok(words);
    }

    @GetMapping("/meanings")
    @Operation(summary = "Get meanings by their IDs")
    public ResponseEntity<List<Meaning>> getMeanings(@RequestParam String ids) {
        List<Meaning> meanings = dictionaryCacheService.getMeanings(ids);
        return ResponseEntity.ok(meanings);
    }

    @PostMapping("/cache/clear")
    @Operation(summary = "Clear all caches")
    public ResponseEntity<Void> clearCache() {
        dictionaryCacheService.clearAllCaches();
        return ResponseEntity.ok().build();
    }
}
