package com.zoirs.learn_en_word.service;

import com.zoirs.learn_en_word.client.ChatGPTClient;
import com.zoirs.learn_en_word.config.ChatGPTConfig;
import com.zoirs.learn_en_word.dto.chatgpt.ChatGPTResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatGPTServiceSimpleTest {

    private ChatGPTService chatGPTService;

    @Mock
    private ChatGPTClient chatGPTClient;

    @BeforeEach
    void setUp() {
        chatGPTService = new ChatGPTService(chatGPTClient);
        ReflectionTestUtils.setField(chatGPTService, "model", "gpt-3.5-turbo");
    }

    @Test
    void suggestNewWords_WithValidInput_ReturnsListOfWords() {
        // Given
        Set<String> knownWords = Set.of("apple", "book", "car");
        Set<String> learningWords = Set.of("dog", "elephant", "fruit");

        // Mock the response
        ChatGPTResponse response = new ChatGPTResponse();
        ChatGPTResponse.Choice choice = new ChatGPTResponse.Choice();
        ChatGPTResponse.Message message = new ChatGPTResponse.Message();
        message.setContent("giraffe, kangaroo, watermelon, bicycle, umbrella");
        choice.setMessage(message);
        response.setChoices(List.of(choice));

        when(chatGPTClient.generateResponse(any()))
                .thenReturn(ResponseEntity.ok(response));

        // When
        Set<String> result = chatGPTService.suggestNewWords(knownWords, learningWords);

        // Then
        assertNotNull(result, "Returned words list should not be null");
        assertFalse(result.isEmpty(), "Returned words list should not be empty");
        assertTrue(result.size() >= 3, "Should return at least 3 suggested words");
        
        // Verify each word is not empty
        result.forEach(word -> {
            assertNotNull(word, "Word in the list should not be null");
            assertFalse(word.trim().isEmpty(), "Word should not be empty or whitespace");
        });
    }

    @Test
    void suggestNewWords_WithEmptyInput_ReturnsEmptyList() {
        // Given
        Set<String> emptyList = Set.of();
        when(chatGPTClient.generateResponse(any()))
                .thenReturn(ResponseEntity.ok(new ChatGPTResponse()));

        // When
        Set<String> result = chatGPTService.suggestNewWords(emptyList, emptyList);

        // Then
        assertTrue(result.isEmpty(), "Should return empty list for empty input");
    }
    @Test
    void suggestNewWords_WithEmptyInput_ReturnsEmptyList1() {
        // Given
//        Set<String> emptyList = Set.of();
//        when(chatGPTClient.generateResponse(any()))
//                .thenReturn(ResponseEntity.ok(new ChatGPTResponse()));

        // When
        Set<String> result = chatGPTService.suggestNewWords(Set.of("dog"), Set.of("book", "chair", "like", "apple", "school", "run", "mother"));

        // Then
        assertTrue(result.isEmpty(), "Should return empty list for empty input");
    }

    @Test
    void suggestNewWords_WhenApiFails_ReturnsEmptyList() {
        // Given
        Set<String> knownWords = Set.of("apple");
        Set<String> learningWords = Set.of("banana");
        
        when(chatGPTClient.generateResponse(any()))
                .thenReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());

        // When
        Set<String> result = chatGPTService.suggestNewWords(knownWords, learningWords);

        // Then
        assertNotNull(result, "Should return empty list instead of null");
        assertTrue(result.isEmpty(), "Should return empty list when API fails");
    }
}
