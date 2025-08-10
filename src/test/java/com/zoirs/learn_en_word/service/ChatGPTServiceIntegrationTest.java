package com.zoirs.learn_en_word.service;

import com.zoirs.learn_en_word.TestApplicationRunner;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

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
        Set<String> learningWords = Set.of("water", "like", "school", "run", "mother");

        // When
        Set<String> result = chatGPTService.suggestNewWords(knownWords, learningWords);

        // Then
        assertNotNull(result, "Returned words list should not be null");
        assertFalse(result.isEmpty(), "Returned words list should not be empty");
        assertTrue(result.size() >= 3, "Should return at least 3 suggested words");
        
        // Log the result for visibility
        log.info("Suggested words: {}", String.join(", ", result));
        
        // Verify each word is not empty
        result.forEach(word -> {
            assertNotNull(word, "Word in the list should not be null");
            assertFalse(word.trim().isEmpty(), "Word should not be empty or whitespace");
        });
    }

}
