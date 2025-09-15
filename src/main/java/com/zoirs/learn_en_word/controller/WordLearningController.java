package com.zoirs.learn_en_word.controller;

import com.zoirs.learn_en_word.entity.UserWord;
import com.zoirs.learn_en_word.service.WordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/words/learning")
@RequiredArgsConstructor
public class WordLearningController {
    
    private final WordService wordService;
    
//    @GetMapping("/review")
//    public ResponseEntity<List<UserWord>> getWordsForReview(@RequestParam Long userId) {
//        wordService.ensureEnoughWordsForLearning(userId);
//        List<UserWord> wordsForReview = wordService.getWordsForReview(userId);
//        return ResponseEntity.ok(wordsForReview);
//    }
    
//    @PostMapping("/answer")
//    public ResponseEntity<UserWord> processAnswer(
//            @RequestParam Long userId,
//            @RequestParam Long wordId,
//            @RequestParam boolean isCorrect) {
//
//        UserWord updatedUserWord = wordService.processAnswer(userId, wordId, isCorrect);
//        return ResponseEntity.ok(updatedUserWord);
//    }
//
//    @GetMapping("/progress/{wordId}")
//    public ResponseEntity<UserWord> getWordProgress(
//            @RequestParam Long userId,
//            @PathVariable Long wordId) {
//
//        UserWord userWord = wordService.getUserWordProgress(userId, wordId);
//        return ResponseEntity.ok(userWord);
//    }
}
