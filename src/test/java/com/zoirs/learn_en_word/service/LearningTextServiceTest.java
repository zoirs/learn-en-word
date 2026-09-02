package com.zoirs.learn_en_word.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zoirs.learn_en_word.client.ChatGPTClient;
import com.zoirs.learn_en_word.dto.chatgpt.ChatGPTRequest;
import com.zoirs.learn_en_word.dto.chatgpt.ChatGPTResponse;
import com.zoirs.learn_en_word.dto.text.GeneratedTextResponse;
import com.zoirs.learn_en_word.entity.User;
import com.zoirs.learn_en_word.model.MeaningEntity;
import com.zoirs.learn_en_word.repository.MeaningRepository;
import com.zoirs.learn_en_word.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearningTextServiceTest {

    private static final String VALID_TEXT = "Every morning, I eat an apple before I travel to work. "
            + "The familiar habit gives me energy, and the journey helps me notice small details in the city. "
            + "I carry a book, meet a friend, and practice English while I wait for the bus. "
            + "On the way, I think about my plans for the day and choose one useful goal. "
            + "After work, I review what I learned and write a short message about it. "
            + "This simple routine keeps me calm, curious, and ready to improve.";

    private static final String VALID_TEXT_WITH_ALL_LEVEL_WORDS = "At work, I put an apple beside my book while I "
            + "considered my career and a difficult solution. I wanted to travel, so I wrote a coherent plan to "
            + "justify the expense. I knew that poor preparation could undermine the trip, so I checked every "
            + "detail carefully. Later, I discussed the idea with a friend, revised my notes, and chose a practical "
            + "route that matched my budget and schedule.";

    private LearningTextService learningTextService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MeaningRepository meaningRepository;

    @Mock
    private ChatGPTClient chatGPTClient;

    @BeforeEach
    void setUp() {
        learningTextService = new LearningTextService(
                userRepository,
                meaningRepository,
                chatGPTClient,
                new ObjectMapper()
        );
    }

    @Test
    void generateText_UsesKnownAndLearningSingleWordsAndUserLevel() {
        User user = user(Set.of(1, 2), Set.of(3, 4));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(meaningRepository.findByExternalIdIn(anyList())).thenReturn(List.of(
                meaning(1, "apple", 1, "n"),
                meaning(2, "good morning", 1, "ph"),
                meaning(3, "travel", 2, "v"),
                meaning(4, "take care", 2, "phi")
        ));
        when(chatGPTClient.generateResponse(any())).thenReturn(chatGptResponse(VALID_TEXT));

        GeneratedTextResponse result = learningTextService.generateText("user-1").orElseThrow();

        assertEquals(VALID_TEXT, result.text());
        assertEquals(List.of(1), result.knownMeaningIds());
        assertEquals(List.of(3), result.learningMeaningIds());
        assertTrue(result.text().length() >= 400 && result.text().length() <= 500);

        ArgumentCaptor<ChatGPTRequest> requestCaptor = ArgumentCaptor.forClass(ChatGPTRequest.class);
        verify(chatGPTClient).generateResponse(requestCaptor.capture());
        String systemPrompt = requestCaptor.getValue().getMessages().getFirst().getContent();
        String prompt = requestCaptor.getValue().getMessages().get(1).getContent();
        assertTrue(systemPrompt.contains("Treat both word lists as optional vocabulary pools, not checklists"));
        assertTrue(systemPrompt.contains("Use only words that fit the topic naturally; ignore the rest"));
        assertTrue(systemPrompt.contains("Prioritize natural English, clear logic, and correct collocations"));
        assertTrue(systemPrompt.contains("Do not add sentences or details only to force a listed word"));
        assertTrue(prompt.contains("CEFR level: A2"));
        assertTrue(prompt.contains("Known-word pool: apple"));
        assertTrue(prompt.contains("Learning-word pool: travel"));
        assertTrue(prompt.contains("Most words in both pools may remain unused"));
        assertFalse(prompt.contains("good morning"));
        assertFalse(prompt.contains("take care"));
    }

    @Test
    void generateText_DoesNotTreatAdvancedLearningWordsAsTheUsersCurrentLevel() {
        User user = user(Set.of(1, 2, 3, 4), Set.of(5, 6, 7, 8));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(meaningRepository.findByExternalIdIn(anyList())).thenReturn(List.of(
                meaning(1, "apple", 1, "n"),
                meaning(2, "book", 1, "n"),
                meaning(3, "career", 2, "n"),
                meaning(4, "solution", 2, "n"),
                meaning(5, "travel", 4, "v"),
                meaning(6, "coherent", 4, "j"),
                meaning(7, "justify", 4, "v"),
                meaning(8, "undermine", 4, "v")
        ));
        when(chatGPTClient.generateResponse(any())).thenReturn(chatGptResponse(VALID_TEXT_WITH_ALL_LEVEL_WORDS));

        GeneratedTextResponse result = learningTextService.generateText("user-1").orElseThrow();

        assertEquals(2, result.knownMeaningIds().size());
        assertEquals(2, result.learningMeaningIds().size());
        assertTrue(Set.of(1, 2, 3, 4).containsAll(result.knownMeaningIds()));
        assertTrue(Set.of(5, 6, 7, 8).containsAll(result.learningMeaningIds()));

        ArgumentCaptor<ChatGPTRequest> requestCaptor = ArgumentCaptor.forClass(ChatGPTRequest.class);
        verify(chatGPTClient).generateResponse(requestCaptor.capture());
        String prompt = requestCaptor.getValue().getMessages().get(1).getContent();
        assertTrue(prompt.contains("CEFR level: B1"));
        assertFalse(prompt.contains("CEFR level: C1"));
        assertEquals(2, promptPool(prompt, "Known-word pool: ").size());
        assertEquals(2, promptPool(prompt, "Learning-word pool: ").size());
    }

    @Test
    void generateText_LimitsEachRandomWordPoolToThirtyWords() {
        Set<Integer> knownIds = new java.util.LinkedHashSet<>();
        Set<Integer> learningIds = new java.util.LinkedHashSet<>();
        List<MeaningEntity> meanings = new ArrayList<>();
        for (int id = 1; id <= 80; id++) {
            knownIds.add(id);
            meanings.add(meaning(id, "known" + id, 1, "n"));
        }
        for (int id = 101; id <= 180; id++) {
            learningIds.add(id);
            meanings.add(meaning(id, "learning" + id, 2, "n"));
        }

        when(userRepository.findById("user-1")).thenReturn(Optional.of(user(knownIds, learningIds)));
        when(meaningRepository.findByExternalIdIn(anyList())).thenReturn(meanings);
        when(chatGPTClient.generateResponse(any())).thenReturn(ResponseEntity.status(429).body("{}"));

        assertTrue(learningTextService.generateText("user-1").isEmpty());

        ArgumentCaptor<ChatGPTRequest> requestCaptor = ArgumentCaptor.forClass(ChatGPTRequest.class);
        verify(chatGPTClient, times(2)).generateResponse(requestCaptor.capture());
        String prompt = requestCaptor.getAllValues().getFirst().getMessages().get(1).getContent();
        assertEquals(30, promptPool(prompt, "Known-word pool: ").size());
        assertEquals(30, promptPool(prompt, "Learning-word pool: ").size());
    }

    @Test
    void generateText_RetriesWhenFirstTextDoesNotMeetConstraints() {
        User user = user(Set.of(1), Set.of(2));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(meaningRepository.findByExternalIdIn(anyList())).thenReturn(List.of(
                meaning(1, "apple", 1, "n"),
                meaning(2, "travel", 2, "v")
        ));
        when(chatGPTClient.generateResponse(any()))
                .thenReturn(chatGptResponse("An apple can travel."))
                .thenReturn(chatGptResponse(VALID_TEXT));

        GeneratedTextResponse result = learningTextService.generateText("user-1").orElseThrow();

        assertEquals(VALID_TEXT, result.text());
        assertEquals(List.of(1), result.knownMeaningIds());
        assertEquals(List.of(2), result.learningMeaningIds());
        verify(chatGPTClient, times(2)).generateResponse(any());
    }

    @Test
    void generateText_WhenUserDoesNotExist_ReturnsEmptyResult() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        Optional<GeneratedTextResponse> result = learningTextService.generateText("missing");

        assertTrue(result.isEmpty());
        verify(meaningRepository, never()).findByExternalIdIn(anyList());
        verify(chatGPTClient, never()).generateResponse(any());
    }

    @Test
    void generateText_WhenUserIdIsBlank_ReturnsEmptyResult() {
        Optional<GeneratedTextResponse> result = learningTextService.generateText(" ");

        assertTrue(result.isEmpty());
        verify(userRepository, never()).findById(any());
        verify(meaningRepository, never()).findByExternalIdIn(anyList());
        verify(chatGPTClient, never()).generateResponse(any());
    }

    @Test
    void generateText_WhenChatGptNeverReturnsValidText_ReturnsEmptyResult() {
        User user = user(Set.of(1), Set.of(2));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(meaningRepository.findByExternalIdIn(anyList())).thenReturn(List.of(
                meaning(1, "apple", 1, "n"),
                meaning(2, "travel", 2, "v")
        ));
        when(chatGPTClient.generateResponse(any())).thenReturn(chatGptResponse("An apple can travel."));

        Optional<GeneratedTextResponse> result = learningTextService.generateText("user-1");

        assertTrue(result.isEmpty());
        verify(chatGPTClient, times(2)).generateResponse(any());
    }

    @Test
    void generateText_WhenChatGptReturnsErrorStatus_ReturnsEmptyResult() {
        User user = user(Set.of(1), Set.of(2));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(meaningRepository.findByExternalIdIn(anyList())).thenReturn(List.of(
                meaning(1, "apple", 1, "n"),
                meaning(2, "travel", 2, "v")
        ));
        when(chatGPTClient.generateResponse(any()))
                .thenReturn(ResponseEntity.status(429).body("""
                        {"error":{"message":"Rate limit exceeded","type":"rate_limit_error","code":"rate_limit_exceeded"}}
                        """));

        Optional<GeneratedTextResponse> result = learningTextService.generateText("user-1");

        assertTrue(result.isEmpty());
        verify(chatGPTClient, times(2)).generateResponse(any());
    }

    @Test
    void generateText_WhenVocabularyIsEmpty_ReturnsEmptyResult() {
        User user = user(Set.of(), Set.of());
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        Optional<GeneratedTextResponse> result = learningTextService.generateText("user-1");

        assertTrue(result.isEmpty());
        verify(meaningRepository, never()).findByExternalIdIn(anyList());
        verify(chatGPTClient, never()).generateResponse(any());
    }

    @Test
    void generateText_WhenOneVocabularyGroupIsEmpty_ReturnsEmptyResult() {
        User user = user(Set.of(1), Set.of());
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        Optional<GeneratedTextResponse> result = learningTextService.generateText("user-1");

        assertTrue(result.isEmpty());
        verify(meaningRepository, never()).findByExternalIdIn(anyList());
        verify(chatGPTClient, never()).generateResponse(any());
    }

    @Test
    void generateText_WhenOneVocabularyGroupContainsOnlyPhrases_ReturnsEmptyResult() {
        User user = user(Set.of(1), Set.of(2));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(meaningRepository.findByExternalIdIn(anyList())).thenReturn(List.of(
                meaning(1, "good morning", 1, "ph"),
                meaning(2, "travel", 2, "v")
        ));

        Optional<GeneratedTextResponse> result = learningTextService.generateText("user-1");

        assertTrue(result.isEmpty());
        verify(chatGPTClient, never()).generateResponse(any());
    }

    private User user(Set<Integer> knownWords, Set<Integer> learningWords) {
        User user = new User();
        user.setId("user-1");
        user.setKnownWords(knownWords);
        user.setLearningWords(learningWords);
        return user;
    }

    private MeaningEntity meaning(Integer externalId, String text, Integer level, String partOfSpeechCode) {
        MeaningEntity meaning = new MeaningEntity();
        meaning.setExternalId(externalId);
        meaning.setText(text);
        meaning.setDifficultyLevel(level);
        meaning.setPartOfSpeechCode(partOfSpeechCode);
        meaning.setPopularity(1d);
        return meaning;
    }

    private ResponseEntity<String> chatGptResponse(String text) {
        ChatGPTResponse.Message message = new ChatGPTResponse.Message();
        message.setContent("{\"text\":\"" + text + "\"}");
        ChatGPTResponse.Choice choice = new ChatGPTResponse.Choice();
        choice.setMessage(message);
        choice.setFinish_reason("stop");
        ChatGPTResponse response = new ChatGPTResponse();
        response.setChoices(List.of(choice));
        try {
            return ResponseEntity.ok(new ObjectMapper().writeValueAsString(response));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private List<String> promptPool(String prompt, String prefix) {
        String line = prompt.lines()
                .filter(value -> value.startsWith(prefix))
                .findFirst()
                .orElseThrow();
        return List.of(line.substring(prefix.length(), line.length() - 1).split(", "));
    }
}
