package com.zoirs.learn_en_word.controller;

import com.zoirs.learn_en_word.dto.text.GeneratedTextResponse;
import com.zoirs.learn_en_word.service.LearningTextService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/texts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Learning Text", description = "API for generating texts based on a user's vocabulary")
public class TextController {

    private final LearningTextService learningTextService;

    @PostMapping("/generate/{userId}")
    @Operation(
            summary = "Generate a learning text",
            description = "Generate a 400-500 character English text and return the known and learning meaning IDs used in it"
    )
    public ResponseEntity<GeneratedTextResponse> generateText(@PathVariable String userId) {
        log.info("Learning text generation requested for userId={}", userId);
        Optional<GeneratedTextResponse> generatedText = learningTextService.generateText(userId);
        if (generatedText.isEmpty()) {
            log.warn("Learning text was not generated for userId={}: no valid result", userId);
            return ResponseEntity.noContent().build();
        }

        log.info("Learning text generated successfully for userId={}", userId);
        return ResponseEntity.ok(generatedText.get());
    }
}
