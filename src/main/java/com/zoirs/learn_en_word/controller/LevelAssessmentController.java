package com.zoirs.learn_en_word.controller;

import com.zoirs.learn_en_word.service.ChatGPTService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/level-assessment")
@RequiredArgsConstructor
@Tag(name = "Level Assessment", description = "API for English proficiency level assessment")
public class LevelAssessmentController {

    private final ChatGPTService chatGPTService;

    @GetMapping("/initial-words")
    @Operation(summary = "Get initial assessment words",
              description = "Get a set of words to assess English proficiency level")
    public ResponseEntity<List<String>> getInitialAssessmentWords() {
        List<String> assessmentWords = chatGPTService.getInitialAssessmentWords();
        if (assessmentWords == null || assessmentWords.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(assessmentWords);
    }

//    @PostMapping("/evaluate")
//    @Operation(summary = "Evaluate assessment results",
//              description = "Evaluate user's responses to assessment words and determine English level")
//    public ResponseEntity<Map<String, Object>> evaluateAssessment(
//            @RequestBody Map<String, Boolean> responses
//    ) {
//        Map<String, Object> evaluationResult = chatGPTService.evaluateAssessment(responses);
//        if (evaluationResult == null || evaluationResult.isEmpty()) {
//            return ResponseEntity.noContent().build();
//        }
//        return ResponseEntity.ok(evaluationResult);
//    }
}
