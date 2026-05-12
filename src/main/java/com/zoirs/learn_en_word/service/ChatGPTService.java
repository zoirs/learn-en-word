package com.zoirs.learn_en_word.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zoirs.learn_en_word.client.ChatGPTClient;
import com.zoirs.learn_en_word.dto.chatgpt.ChatGPTRequest;
import com.zoirs.learn_en_word.dto.chatgpt.ChatGPTResponse;
import com.zoirs.learn_en_word.dto.chatgpt.WordsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatGPTService {

    private static final int MAX_CONTEXT_WORDS = 100;

    private static final Map<String, Object> SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of(
                    "easier", Map.of(
                            "type", "array",
                            "items", Map.of("type", "string")
                    ),
                    "same", Map.of(
                            "type", "array",
                            "items", Map.of("type", "string")
                    ),
                    "harder", Map.of(
                            "type", "array",
                            "items", Map.of("type", "string")
                    )
            ),
            "required", List.of("easier", "same", "harder"),
            "additionalProperties", false
    );

    private static final String SYSTEM_PROMPT = """
            You are a helpful English language learning assistant.
            Your task is to suggest new English words for a learner based on the words they already know and the words they are currently learning.
            Each word must follow these rules:
            1. Avoid rare, academic words.
            2. Avoid synonyms and near-synonyms of each other and of the user's known/learning words.
            3. Suggest words in three groups:
            - 3 words that are slightly easier than the current learning words,
            - 3 words at approximately the same level as the current learning words,
            - 3 words that are slightly more advanced.
            4. Each group must contain 1 noun, 1 verb, 1 adverb or adjective.
            Return only JSON matching the requested schema.
            """;

    private final ChatGPTClient chatGPTClient;
    private final ObjectMapper objectMapper;

    public Set<String> suggestNewWords(Set<String> knownWords, Set<String> learningWords) {
        try {
            Set<String> normalizedKnownWords = normalizeWords(knownWords);
            Set<String> normalizedLearningWords = normalizeWords(learningWords);
            if (normalizedLearningWords.isEmpty()) {
                log.info("Skipping ChatGPT word suggestions because learning words are empty");
                return Collections.emptySet();
            }

            String prompt = String.format(
                    "I know these words well: %s. I'm currently learning these words: %s. " +
                            "Please suggest new words for me to learn next.",
                    String.join(", ", limitWords(normalizedKnownWords)),
                    String.join(", ", limitWords(normalizedLearningWords))
            );

            Map<String, Object> responseFormat = Map.of(
                    "type", "json_schema",
                    "json_schema", Map.of(
                            "name", "word_suggestion",
                            "strict", true,
                            "schema", SCHEMA
                    )
            );
            ChatGPTRequest request = new ChatGPTRequest(
                    List.of(
                            ChatGPTRequest.Message.systemMessage(SYSTEM_PROMPT),
                            ChatGPTRequest.Message.userMessage(prompt)
                    ),
                    0.7,
                    responseFormat
            );
            log.info("Request: {}", prompt);
            ResponseEntity<ChatGPTResponse> response = chatGPTClient.generateResponse(request);
            if (response == null) {
                log.warn("ChatGPT client returned null response");
                return Collections.emptySet();
            }

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("ChatGPT returned non-success status: {}", response.getStatusCode());
                return Collections.emptySet();
            }

            ChatGPTResponse body = response.getBody();
            if (body == null || body.getChoices() == null || body.getChoices().isEmpty()) {
                return Collections.emptySet();
            }

            ChatGPTResponse.Choice choice = body.getChoices().getFirst();
            if (choice == null || choice.getMessage() == null || choice.getMessage().getContent() == null) {
                return Collections.emptySet();
            }

            if (!"stop".equals(choice.getFinish_reason())) {
                log.warn("ChatGPT response finish reason: {}", choice.getFinish_reason());
            }

            String content = choice.getMessage().getContent();
            log.info("Response: {}", content);
            WordsResponse wordsResponse = objectMapper.readValue(content, WordsResponse.class);
            WordSuggestionLimits.GroupLimits limits = WordSuggestionLimits
                    .forLearningWordsCount(learningWords == null ? 0 : learningWords.size())
                    .chatGpt();

            Set<String> excludedWords = Stream.concat(normalizedKnownWords.stream(), normalizedLearningWords.stream())
                    .collect(Collectors.toSet());
            Set<String> suggestions = new LinkedHashSet<>();
            suggestions.addAll(filterWords(wordsResponse.getEasier(), excludedWords, limits.easier()));
            suggestions.addAll(filterWords(wordsResponse.getSame(), excludedWords, limits.same()));
            suggestions.addAll(filterWords(wordsResponse.getHarder(), excludedWords, limits.harder()));
            return suggestions;
        } catch (Exception e) {
            log.error("Error while getting word suggestions from ChatGPT", e);
            return Collections.emptySet();
        }
    }

    private static List<String> filterWords(List<String> words, Set<String> excludedWords, int limit) {
        if (words == null || words.isEmpty() || limit <= 0) {
            return Collections.emptyList();
        }
        return words.stream()
                    .map(ChatGPTService::normalizeWord)
                    .filter(word -> !word.isBlank())
                    .filter(word -> !excludedWords.contains(word))
                    .filter(ChatGPTService::isSingleWord)
                    .distinct()
                    .limit(limit)
                    .toList();
    }

    private static Set<String> normalizeWords(Set<String> words) {
        if (words == null || words.isEmpty()) {
            return Collections.emptySet();
        }
        return words.stream()
                .map(ChatGPTService::normalizeWord)
                .filter(word -> !word.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static String normalizeWord(String word) {
        return word == null ? "" : word.trim().toLowerCase(Locale.ROOT);
    }

    private static List<String> limitWords(Set<String> words) {
        return words.stream()
                .sorted()
                .limit(MAX_CONTEXT_WORDS)
                .toList();
    }

    private static boolean isSingleWord(String word) {
        return word.chars().noneMatch(Character::isWhitespace);
    }
}
