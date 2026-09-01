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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class LearningTextService {

    private static final int MIN_TEXT_LENGTH = 400;
    private static final int MAX_TEXT_LENGTH = 500;
    private static final int MAX_KNOWN_WORDS = 100;
    private static final int MAX_LEARNING_WORDS = 30;
    private static final int MAX_GENERATION_ATTEMPTS = 2;
    private static final Set<String> PHRASE_PART_OF_SPEECH_CODES = Set.of("ph", "phi");

    private static final Map<String, Object> RESPONSE_SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of("text", Map.of("type", "string")),
            "required", List.of("text"),
            "additionalProperties", false
    );

    private static final String SYSTEM_PROMPT = """
            You create short reading passages for learners of English.
            Return only JSON matching the requested schema.
            The text must be one coherent, natural English paragraph without a title, notes, or Markdown.
            Its total length must be between 400 and 500 characters inclusive, counting spaces and punctuation.
            Match the requested CEFR level. Use vocabulary and grammar appropriate for that level.
            Treat both word lists as vocabulary pools, not as checklists.
            Choose only a small, semantically compatible subset that fits one clear topic. Do not try to use every listed word or force unrelated words into the paragraph.
            Include at least one word from each pool and use the listed spelling exactly. Prefer several learning words only when they fit naturally.
            Coherence and natural meaning are more important than the number of included words.
            """;

    private static final Comparator<MeaningEntity> WORD_ORDER = Comparator
            .comparing(MeaningEntity::getPopularity, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(MeaningEntity::getText, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));

    private final UserRepository userRepository;
    private final MeaningRepository meaningRepository;
    private final ChatGPTClient chatGPTClient;
    private final ObjectMapper objectMapper;

    public Optional<GeneratedTextResponse> generateText(String userId) {
        if (userId == null || userId.isBlank()) {
            return Optional.empty();
        }

        try {
            Optional<User> userOptional = userRepository.findById(userId);
            if (userOptional.isEmpty()) {
                return Optional.empty();
            }
            User user = userOptional.get();
            Set<Integer> learningIds = normalizeIds(user.getLearningWords());
            Set<Integer> knownIds = normalizeIds(user.getKnownWords());
            knownIds.removeAll(learningIds);

            if (learningIds.isEmpty() || knownIds.isEmpty()) {
                return Optional.empty();
            }

            Set<Integer> allIds = new LinkedHashSet<>(learningIds);
            allIds.addAll(knownIds);

            List<MeaningEntity> meanings = meaningRepository.findByExternalIdIn(new ArrayList<>(allIds));
            List<VocabularyWord> learningWords = selectWords(meanings, learningIds, MAX_LEARNING_WORDS);
            List<VocabularyWord> knownWords = selectWords(meanings, knownIds, MAX_KNOWN_WORDS);
            if (learningWords.isEmpty() || knownWords.isEmpty()) {
                return Optional.empty();
            }

            String cefrLevel = calculateCefrLevel(meanings, knownIds);
            String prompt = """
                    CEFR level: %s.
                    Known-word pool: %s.
                    Learning-word pool: %s.
                    Write the requested English paragraph now.
                    """.formatted(
                    cefrLevel,
                    joinWords(knownWords),
                    joinWords(learningWords)
            );

            log.info(
                    "Generating learning text for userId {}: level={}, knownWords={}, learningWords={}",
                    userId,
                    cefrLevel,
                    knownWords.size(),
                    learningWords.size()
            );
            return generateValidText(userId, prompt, knownWords, learningWords);
        } catch (Exception e) {
            log.error("Failed to generate a learning text for userId={}", userId, e);
            return Optional.empty();
        }
    }

    private Optional<GeneratedTextResponse> generateValidText(
            String userId,
            String prompt,
            List<VocabularyWord> knownWords,
            List<VocabularyWord> learningWords
    ) {
        String currentPrompt = prompt;
        for (int attempt = 1; attempt <= MAX_GENERATION_ATTEMPTS; attempt++) {
            String result = requestText(userId, attempt, currentPrompt);
            if (isValid(result, knownWords, learningWords)) {
                String text = result.trim();
                log.info(
                        "Generated valid learning text: userId={}, attempt={}, characters={}",
                        userId,
                        attempt,
                        characterCount(text)
                );
                return Optional.of(new GeneratedTextResponse(
                        text,
                        findUsedMeaningIds(text, knownWords),
                        findUsedMeaningIds(text, learningWords)
                ));
            }

            if (result != null) {
                String normalizedText = result.trim();
                log.warn(
                        "ChatGPT returned an invalid learning text: userId={}, attempt={}, characters={}, "
                                + "latinOnly={}, containsKnownWord={}, containsLearningWord={}",
                        userId,
                        attempt,
                        characterCount(normalizedText),
                        containsOnlyLatinLetters(normalizedText),
                        containsAnyWord(normalizedText, knownWords),
                        containsAnyWord(normalizedText, learningWords)
                );
            }
            currentPrompt = prompt + "\nThe previous attempt was invalid. Carefully satisfy every constraint, especially the 400-500 character limit and inclusion of at least one word from each pool. Use only a coherent subset; do not force every word into the paragraph.";
        }
        log.warn(
                "Learning text generation produced no valid result: userId={}, attempts={}",
                userId,
                MAX_GENERATION_ATTEMPTS
        );
        return Optional.empty();
    }

    private String requestText(String userId, int attempt, String prompt) {
        Map<String, Object> responseFormat = Map.of(
                "type", "json_schema",
                "json_schema", Map.of(
                        "name", "learning_text",
                        "strict", true,
                        "schema", RESPONSE_SCHEMA
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

        try {
            ResponseEntity<String> response = chatGPTClient.generateResponse(request);
            if (response == null) {
                log.warn(
                        "ChatGPT client returned a null response: userId={}, attempt={}",
                        userId,
                        attempt
                );
                return null;
            }
            if (response.getStatusCode().value() != 200) {
                log.warn(
                        "ChatGPT request returned a non-success status: userId={}, attempt={}, status={}, responseBody={}",
                        userId,
                        attempt,
                        response.getStatusCode().value(),
                        response.getBody()
                );
                return null;
            }

            if (response.getBody() == null || response.getBody().isBlank()) {
                log.warn(
                        "ChatGPT returned an empty body: userId={}, attempt={}, status={}",
                        userId,
                        attempt,
                        response.getStatusCode().value()
                );
                return null;
            }

            ChatGPTResponse body = objectMapper.readValue(response.getBody(), ChatGPTResponse.class);
            if (body == null || body.getChoices() == null || body.getChoices().isEmpty()) {
                log.warn(
                        "ChatGPT returned no choices: userId={}, attempt={}, status={}",
                        userId,
                        attempt,
                        response.getStatusCode().value()
                );
                return null;
            }

            ChatGPTResponse.Choice choice = body.getChoices().getFirst();
            if (choice == null || choice.getMessage() == null || choice.getMessage().getContent() == null) {
                log.warn(
                        "ChatGPT returned an empty choice: userId={}, attempt={}, finishReason={}",
                        userId,
                        attempt,
                        choice == null ? null : choice.getFinish_reason()
                );
                return null;
            }
            var content = objectMapper.readTree(choice.getMessage().getContent());
            String text = content == null ? null : content.path("text").textValue();
            if (text == null) {
                log.warn(
                        "ChatGPT response JSON has no text field: userId={}, attempt={}, finishReason={}",
                        userId,
                        attempt,
                        choice.getFinish_reason()
                );
            }
            return text;
        } catch (RestClientResponseException e) {
            log.error(
                    "ChatGPT HTTP request failed: userId={}, attempt={}, status={}, responseBody={}",
                    userId,
                    attempt,
                    e.getStatusCode().value(),
                    e.getResponseBodyAsString(),
                    e
            );
            return null;
        } catch (Exception e) {
            log.error(
                    "ChatGPT request failed: userId={}, attempt={}, exceptionType={}, message={}",
                    userId,
                    attempt,
                    e.getClass().getSimpleName(),
                    e.getMessage(),
                    e
            );
            return null;
        }
    }

    private List<VocabularyWord> selectWords(List<MeaningEntity> meanings, Set<Integer> ids, int limit) {
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, VocabularyWord> uniqueWords = new LinkedHashMap<>();
        meanings.stream()
                .filter(meaning -> ids.contains(meaning.getExternalId()))
                .filter(meaning -> meaning.getExternalId() != null)
                .filter(this::isSingleWord)
                .sorted(WORD_ORDER)
                .forEach(meaning -> {
                    String word = meaning.getText().trim().toLowerCase(Locale.ROOT);
                    uniqueWords.putIfAbsent(
                            word,
                            new VocabularyWord(meaning.getExternalId(), word)
                    );
                });
        return uniqueWords.values().stream()
                .limit(limit)
                .toList();
    }

    private String joinWords(List<VocabularyWord> words) {
        return words.stream()
                .map(VocabularyWord::text)
                .collect(java.util.stream.Collectors.joining(", "));
    }

    private boolean isSingleWord(MeaningEntity meaning) {
        String text = meaning.getText();
        String partOfSpeechCode = meaning.getPartOfSpeechCode();
        return text != null
                && !text.isBlank()
                && text.codePoints().noneMatch(Character::isWhitespace)
                && (partOfSpeechCode == null
                || !PHRASE_PART_OF_SPEECH_CODES.contains(partOfSpeechCode.trim().toLowerCase(Locale.ROOT)));
    }

    private String calculateCefrLevel(List<MeaningEntity> meanings, Set<Integer> knownIds) {
        List<Integer> levels = meanings.stream()
                .filter(meaning -> knownIds.contains(meaning.getExternalId()))
                .filter(this::isSingleWord)
                .map(MeaningEntity::getDifficultyLevel)
                .filter(Objects::nonNull)
                .filter(level -> level >= 0 && level <= 5)
                .toList();
        if (levels.isEmpty()) {
            levels = meanings.stream()
                    .filter(this::isSingleWord)
                    .map(MeaningEntity::getDifficultyLevel)
                    .filter(Objects::nonNull)
                    .filter(level -> level >= 0 && level <= 5)
                    .toList();
        }
        if (levels.isEmpty()) {
            return "infer it from the supplied vocabulary";
        }

        int roundedLevel = (int) Math.round(levels.stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0));
        return List.of("A1", "A2", "B1", "B2", "C1", "C2").get(roundedLevel);
    }

    private boolean isValid(
            String text,
            List<VocabularyWord> knownWords,
            List<VocabularyWord> learningWords
    ) {
        if (text == null) {
            return false;
        }
        String normalizedText = text.trim();
        int length = characterCount(normalizedText);
        return length >= MIN_TEXT_LENGTH
                && length <= MAX_TEXT_LENGTH
                && containsOnlyLatinLetters(normalizedText)
                && containsAnyWord(normalizedText, knownWords)
                && containsAnyWord(normalizedText, learningWords);
    }

    private int characterCount(String text) {
        return text.codePointCount(0, text.length());
    }

    private boolean containsOnlyLatinLetters(String text) {
        return text.codePoints()
                .filter(Character::isLetter)
                .allMatch(codePoint -> Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.LATIN);
    }

    private boolean containsAnyWord(String text, List<VocabularyWord> words) {
        return words.stream().anyMatch(word -> containsWord(text, word.text()));
    }

    private List<Integer> findUsedMeaningIds(String text, List<VocabularyWord> words) {
        return words.stream()
                .filter(word -> containsWord(text, word.text()))
                .map(VocabularyWord::meaningId)
                .distinct()
                .toList();
    }

    private boolean containsWord(String text, String word) {
        Pattern pattern = Pattern.compile(
                "(?<![\\p{L}\\p{N}'\\x{2019}-])"
                        + Pattern.quote(word)
                        + "(?![\\p{L}\\p{N}'\\x{2019}-])",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
        );
        return pattern.matcher(text).find();
    }

    private Set<Integer> normalizeIds(Set<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return new LinkedHashSet<>();
        }
        return ids.stream()
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private record VocabularyWord(Integer meaningId, String text) {
    }
}
