package com.zoirs.learn_en_word.api.service;

import com.zoirs.learn_en_word.LearnEnWordApplication;
import com.zoirs.learn_en_word.api.dto.skyeng.Meaning;
import com.zoirs.learn_en_word.api.dto.skyeng.Word;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = LearnEnWordApplication.class)
@ActiveProfiles("test")
class SkyengDictionaryServiceIntegrationTest {

    @Autowired
    private SkyengDictionaryService dictionaryService;

    @Test
    void searchWords_shouldReturnNonEmptyList_whenSearchingForExistingWord() {
        // Arrange
        String searchQuery = "hello";

        // Act
        List<Word> result = dictionaryService.searchWords(searchQuery);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertFalse(result.isEmpty(), "Result list should not be empty");
        
        // Verify the first word contains the search query (case-insensitive)
        Word firstWord = result.get(0);
        assertNotNull(firstWord.getText(), "Word text should not be null");
        assertTrue(firstWord.getText().toLowerCase().contains(searchQuery.toLowerCase()),
                "Word should contain the search query");
        
        // Verify meanings are present
        assertNotNull(firstWord.getMeanings(), "Meanings list should not be null");
        assertFalse(firstWord.getMeanings().isEmpty(), "Meanings list should not be empty");
    }

    @Test
    void getMeanings_shouldReturnMeanings_whenValidIdsProvided() {
        // Arrange
        // These are known meaning IDs from the Skyeng API
        String meaningIds = "1,2,3";

        // Act
        List<Meaning> result = dictionaryService.getMeanings(meaningIds);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertFalse(result.isEmpty(), "Result list should not be empty");
        
        // Verify each meaning has required fields
        for (Meaning meaning : result) {
            assertNotNull(meaning.getId(), "Meaning ID should not be null");
            assertNotNull(meaning.getText(), "Meaning text should not be null");
            assertNotNull(meaning.getPartOfSpeechCode(), "Part of speech code should not be null");
            assertNotNull(meaning.getTranslation(), "Translation should not be null");
            assertNotNull(meaning.getTranslation().getText(), "Translation text should not be null");
        }
    }

    @Test
    void getUpdatedMeanings_shouldReturnEmptyList_whenNoUpdates() {
        // Arrange - using a future date to ensure no updates
        String futureDate = "2100-01-01T00:00:00Z";

        // Act
        List<Meaning> result = dictionaryService.getUpdatedMeanings(futureDate);

        // Assert - should return empty list when no updates found
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isEmpty(), "Result list should be empty for future date");
    }

    @Test
    void searchWords_shouldReturnEmptyList_whenSearchingForNonExistentWord() {
        // Arrange
        String searchQuery = "nonexistentword123";

        // Act
        List<Word> result = dictionaryService.searchWords(searchQuery);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isEmpty(), "Result list should be empty for non-existent word");
    }
}
