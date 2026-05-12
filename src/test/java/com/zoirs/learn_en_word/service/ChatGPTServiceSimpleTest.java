package com.zoirs.learn_en_word.service;

import com.zoirs.learn_en_word.client.ChatGPTClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zoirs.learn_en_word.dto.chatgpt.ChatGPTResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatGPTServiceSimpleTest {

    private ChatGPTService chatGPTService;

    @Mock
    private ChatGPTClient chatGPTClient;

    @BeforeEach
    void setUp() {
        chatGPTService = new ChatGPTService(chatGPTClient, new ObjectMapper());
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
        message.setContent("""
                {"easier":["gift","lend","worry"],"same":["guess","take","careful"],"harder":["chance","return","likely"]}
                """);
        choice.setMessage(message);
        choice.setFinish_reason("stop");
        response.setChoices(List.of(choice));

        when(chatGPTClient.generateResponse(any()))
                .thenReturn(ResponseEntity.ok(response));

        // When
        Set<String> result = chatGPTService.suggestNewWords(knownWords, learningWords);

        // Then
        assertNotNull(result, "Returned words list should not be null");
        assertFalse(result.isEmpty(), "Returned words list should not be empty");
        assertEquals(Set.of("gift", "guess", "take", "chance", "return", "likely"), result);
        
        // Verify each word is not empty
        result.forEach(word -> {
            assertNotNull(word, "Word in the list should not be null");
            assertFalse(word.trim().isEmpty(), "Word should not be empty or whitespace");
        });
    }

    @Test
    void suggestNewWords_WithEmptyInput_ReturnsEmptyList() {
        // Given
        Set<String> emptySet = Set.of();

        // When
        Set<String> result = chatGPTService.suggestNewWords(emptySet, emptySet);

        // Then
        assertTrue(result.isEmpty(), "Should return empty list for empty input");
        verifyNoInteractions(chatGPTClient);
    }

    @Test
    void suggestNewWords_FiltersKnownLearningAndPhrases() {
        // Given
        ChatGPTResponse response = new ChatGPTResponse();
        ChatGPTResponse.Choice choice = new ChatGPTResponse.Choice();
        ChatGPTResponse.Message message = new ChatGPTResponse.Message();
        message.setContent("""
                {"easier":["dog","new word"," Gift "],"same":["book","take","Careful"],"harder":["chance","return","likely"]}
                """);
        choice.setMessage(message);
        choice.setFinish_reason("stop");
        response.setChoices(List.of(choice));

        when(chatGPTClient.generateResponse(any()))
                .thenReturn(ResponseEntity.ok(response));

        // When
        Set<String> result = chatGPTService.suggestNewWords(Set.of("dog"), Set.of("book", "chair"));

        // Then
        assertEquals(Set.of("gift", "take", "careful", "chance", "return", "likely"), result);
    }

    @Test
    void suggestNewWords_AppliesLimitsForCurrentLearningWordsCount() {
        // Given
        ChatGPTResponse response = new ChatGPTResponse();
        ChatGPTResponse.Choice choice = new ChatGPTResponse.Choice();
        ChatGPTResponse.Message message = new ChatGPTResponse.Message();
        message.setContent("""
                {"easier":["easy-one","easy-two","easy-three"],"same":["same-one","same-two","same-three"],"harder":["hard-one","hard-two","hard-three"]}
                """);
        choice.setMessage(message);
        choice.setFinish_reason("stop");
        response.setChoices(List.of(choice));

        when(chatGPTClient.generateResponse(any()))
                .thenReturn(ResponseEntity.ok(response));

        Set<String> learningWords = Set.of(
                "one", "two", "three", "four", "five",
                "six", "seven", "eight", "nine", "ten"
        );

        // When
        Set<String> result = chatGPTService.suggestNewWords(Set.of("known"), learningWords);

        // Then
        assertEquals(Set.of("easy-one", "easy-two", "same-one", "same-two", "hard-one", "hard-two", "hard-three"), result);
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
