package com.zoirs.learn_en_word.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zoirs.learn_en_word.dto.text.GeneratedTextResponse;
import com.zoirs.learn_en_word.service.LearningTextService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TextControllerTest {

    @InjectMocks
    private TextController textController;

    @Mock
    private LearningTextService learningTextService;

    @Test
    void generateText_ReturnsGeneratedEnglishTextAndUsedMeaningIds() throws Exception {
        GeneratedTextResponse generatedText = new GeneratedTextResponse(
                "Generated English text",
                java.util.List.of(1, 2),
                java.util.List.of(3, 4)
        );
        when(learningTextService.generateText("user-1")).thenReturn(Optional.of(generatedText));

        ResponseEntity<GeneratedTextResponse> response = textController.generateText("user-1");

        assertEquals(200, response.getStatusCode().value());
        assertEquals(generatedText, response.getBody());
        assertEquals(
                "{\"text\":\"Generated English text\",\"knownMeaningIds\":[1,2],\"learningMeaningIds\":[3,4]}",
                new ObjectMapper().writeValueAsString(response.getBody())
        );
        verify(learningTextService).generateText("user-1");
    }

    @Test
    void generateText_WhenVocabularyIsEmpty_ReturnsNoContent() {
        when(learningTextService.generateText("user-1")).thenReturn(Optional.empty());

        ResponseEntity<GeneratedTextResponse> response = textController.generateText("user-1");

        assertEquals(204, response.getStatusCode().value());
        assertNull(response.getBody());
        verify(learningTextService).generateText("user-1");
    }
}
