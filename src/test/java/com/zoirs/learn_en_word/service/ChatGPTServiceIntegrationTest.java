package com.zoirs.learn_en_word.service;

import com.zoirs.learn_en_word.TestApplicationRunner;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest(classes = TestApplicationRunner.class)
@ActiveProfiles("test")
class ChatGPTServiceIntegrationTest {

    @Autowired
    private ChatGPTService chatGPTService;

    @Test
    void suggestNewWords_WithValidInput_ReturnsListOfWords() {
        // Given
        Set<String> knownWords = Set.of("dog", "book");
        Set<String> learningWords = new HashSet<>(Set.of("water", "like", "school", "run", "mother", "remember"));

        for (int i = 0; i < 5; i++) {
            Set<String> result = chatGPTService.suggestNewWords(knownWords, learningWords);
            log.info("Suggested words: {}", String.join(",", result));
            learningWords.addAll(result);
        }


    }

}
