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

    private static final int MIN_TEXT_LENGTH = 300;
    private static final int MAX_TEXT_LENGTH = 500;
    private static final int MAX_WORDS_PER_POOL = 30;
    private static final int MAX_GENERATION_ATTEMPTS = 2;
    private static final Set<String> PHRASE_PART_OF_SPEECH_CODES = Set.of("ph", "phi");

    private static final Map<String, Object> RESPONSE_SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of("text", Map.of("type", "string")),
            "required", List.of("text"),
            "additionalProperties", false
    );

    private static final String SYSTEM_PROMPT = """
            Create a short English reading passage for a learner.
            
            Return only JSON matching the requested schema.
            
            Requirements:
            
            * Write one coherent, natural paragraph with no title, notes, or Markdown.
            * Length: 400–500 characters including spaces and punctuation.
            * Match the requested CEFR level.
            * Choose one clear topic and keep all details logically consistent.
            * Treat both word lists as optional vocabulary pools, not checklists.
            * Use at least one word from each pool, with the exact listed spelling.
            * Use only words that fit the topic naturally; ignore the rest.
            * Prioritize natural English, clear logic, and correct collocations over vocabulary coverage.
            * Do not add sentences or details only to force a listed word into the text.
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
            List<VocabularyWord> learningWords = selectWords(meanings, learningIds);
            List<VocabularyWord> knownWords = selectWords(meanings, knownIds);
            if (learningWords.isEmpty() || knownWords.isEmpty()) {
                return Optional.empty();
            }

            String cefrLevel = calculateCefrLevel(meanings, knownIds);
            String prompt = """
                    CEFR level: %s.
                    Known-word pool: %s.
                    Learning-word pool: %s.
                    Select only words that fit one coherent topic. Most words in both pools may remain unused.
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
        for (int attempt = 1; attempt <= MAX_GENERATION_ATTEMPTS; attempt++) {
            String result = requestText(userId, attempt, prompt);
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
                                + "latinOnly={}, usedKnownWords={}, usedLearningWords={}",
                        userId,
                        attempt,
                        characterCount(normalizedText),
                        containsOnlyLatinLetters(normalizedText),
                        countUsedWords(normalizedText, knownWords),
                        countUsedWords(normalizedText, learningWords)
                );
            }
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
                0.4,
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

    private List<VocabularyWord> selectWords(List<MeaningEntity> meanings, Set<Integer> ids) {
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
        List<VocabularyWord> words = new ArrayList<>(uniqueWords.values());
        if (words.isEmpty()) {
            return Collections.emptyList();
        }
        Collections.shuffle(words);
        int selectedWordCount = Math.min(MAX_WORDS_PER_POOL, Math.max(1, words.size() / 2));
        return List.copyOf(words.subList(0, selectedWordCount));
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

    private int countUsedWords(String text, List<VocabularyWord> words) {
        return (int) words.stream()
                .filter(word -> containsWord(text, word.text()))
                .count();
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
